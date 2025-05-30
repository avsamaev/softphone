/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.sipout.phone.ui.main.viewmodel

import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import org.sipout.phone.LinphoneApplication.Companion.coreContext
import org.sipout.phone.LinphoneApplication.Companion.corePreferences
import org.sipout.phone.core.Account
import org.sipout.phone.core.Core
import org.sipout.phone.core.CoreListenerStub
import org.sipout.phone.core.GlobalState
import org.sipout.phone.core.tools.Log
import org.sipout.phone.ui.GenericViewModel
import org.sipout.phone.ui.main.model.AccountModel
import org.sipout.phone.ui.main.model.ShortcutModel
import org.sipout.phone.utils.Event

class DrawerMenuViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Drawer Menu ViewModel]"
    }

    val accounts = MutableLiveData<ArrayList<AccountModel>>()

    val hideAddAccount = MutableLiveData<Boolean>()

    val hideRecordings = MutableLiveData<Boolean>()

    val hideSettings = MutableLiveData<Boolean>()

    val shortcuts = MutableLiveData<ArrayList<ShortcutModel>>()

    val hideQuitButton = MutableLiveData<Boolean>()

    val startAssistantEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val closeDrawerEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val showAccountPopupMenuEvent: MutableLiveData<Event<Pair<View, Account>>> by lazy {
        MutableLiveData<Event<Pair<View, Account>>>()
    }

    val defaultAccountChangedEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    val openLinkInBrowserEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onDefaultAccountChanged(core: Core, account: Account?) {
            if (account == null) {
                Log.w("$TAG Default account is now null!")
            } else {
                Log.i(
                    "$TAG Account [${account.params.identityAddress?.asStringUriOnly()}] has been set as default"
                )
                for (model in accounts.value.orEmpty()) {
                    model.isDefault.postValue(model.account == account)
                }
                defaultAccountChangedEvent.postValue(
                    Event(account.params.identityAddress?.asStringUriOnly() ?: "")
                )
            }
        }

        @WorkerThread
        override fun onAccountAdded(core: Core, account: Account) {
            Log.i(
                "$TAG Account [${account.params.identityAddress?.asStringUriOnly()}] has been added to the Core"
            )
            computeAccountsList()
        }

        @WorkerThread
        override fun onAccountRemoved(core: Core, account: Account) {
            Log.i(
                "$TAG Account [${account.params.identityAddress?.asStringUriOnly()}] has been removed from the Core"
            )
            computeAccountsList()
        }

        @WorkerThread
        override fun onGlobalStateChanged(core: Core, state: GlobalState?, message: String) {
            if (core.globalState == GlobalState.On) {
                accounts.value.orEmpty().forEach(AccountModel::destroy)

                Log.i("$TAG Global state is [${core.globalState}], reload accounts & shortcuts")
                computeAccountsList()
                computeShortcuts()
            }
        }
    }

    init {
        coreContext.postOnCoreThread { core ->
            core.addListener(coreListener)

            hideRecordings.postValue(corePreferences.disableCallRecordings)
            hideSettings.postValue(corePreferences.hideSettings)

            checkIfKeepAliveServiceIsEnabled()

            computeAccountsList()
            computeShortcuts()
        }
    }

    @UiThread
    override fun onCleared() {
        coreContext.postOnCoreThread { core ->
            core.removeListener(coreListener)
            accounts.value.orEmpty().forEach(AccountModel::destroy)
        }

        super.onCleared()
    }

    @UiThread
    fun closeDrawerMenu() {
        closeDrawerEvent.value = Event(true)
    }

    @UiThread
    fun addAccount() {
        startAssistantEvent.value = Event(true)
    }

    @UiThread
    fun updateAccountsList() {
        coreContext.postOnCoreThread {
            computeAccountsList()
        }
    }

    @UiThread
    fun refreshAccountsNotificationsCount() {
        coreContext.postOnCoreThread {
            for (model in accounts.value.orEmpty()) {
                model.computeNotificationsCount()
            }
        }
    }

    @WorkerThread
    fun checkIfKeepAliveServiceIsEnabled() {
        val useKeepAliveService = corePreferences.keepServiceAlive
        hideQuitButton.postValue(!useKeepAliveService)
        if (useKeepAliveService) {
            Log.i("$TAG Keep alive service is enabled, do not hide quit button")
        }
    }

    @WorkerThread
    private fun computeAccountsList() {
        accounts.value.orEmpty().forEach(AccountModel::destroy)

        val list = arrayListOf<AccountModel>()
        for (account in coreContext.core.accountList) {
            val model = AccountModel(account) { view, account ->
                // onClicked
                showAccountPopupMenuEvent.postValue(Event(Pair(view, account)))
            }
            list.add(model)

            if (account == coreContext.core.defaultAccount) {
                defaultAccountChangedEvent.postValue(
                    Event(account.params.identityAddress?.asStringUriOnly() ?: "")
                )
            }
        }
        accounts.postValue(list)

        val maxAccount = corePreferences.maxAccountsCount
        val accountsCount = list.size
        val maxAccountsReached = maxAccount in 1..accountsCount
        if (maxAccountsReached) {
            Log.w(
                "$TAG Max number of allowed accounts reached [$maxAccount], hiding add account button"
            )
        }
        hideAddAccount.postValue(maxAccountsReached)
    }

    @WorkerThread
    private fun computeShortcuts() {
        val config = coreContext.core.config
        val shortcutsList = arrayListOf<ShortcutModel>()
        val shortcutsCount = config.getInt("ui", "shortcut_count", 0)
        if (shortcutsCount > 0) {
            Log.i("$TAG Found [$shortcutsCount] shortcuts to display")
            for (i in 0 until shortcutsCount) {
                val key = "shortcut_$i"
                val label = config.getString(key, "name", "").orEmpty()
                val iconUrl = config.getString(key, "icon", "").orEmpty()
                val linkUrl = config.getString(key, "link", "").orEmpty()
                val shortcutModel = ShortcutModel(
                    label,
                    iconUrl,
                    linkUrl
                ) { link ->
                    openLinkInBrowserEvent.postValue(Event(link))
                }
                shortcutsList.add(shortcutModel)
            }
        }
        shortcuts.postValue(shortcutsList)
    }
}
