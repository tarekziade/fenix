/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.core.net.toUri
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import kotlinx.coroutines.runBlocking
import mozilla.appservices.places.BookmarkRoot
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.ext.bookmarkStorage
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.helpers.TestHelper.longTapSelectItem
import org.mozilla.fenix.ui.robots.bookmarksMenu
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.mDevice
import org.mozilla.fenix.ui.robots.multipleSelectionToolbar
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying basic functionality of bookmarks
 */
class BookmarksTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mockWebServer: MockWebServer
    private val bookmarksFolderName = "New Folder"
    private val bookmarkTitle = "Bookmark title"
    private val bookmarkUrl = "https://www.test.com"

    @get:Rule
    val activityTestRule = HomeActivityTestRule()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        // Clearing all bookmarks data after each test to avoid overlapping data
        val bookmarksStorage = activityTestRule.activity?.bookmarkStorage
        runBlocking {
            val bookmarks = bookmarksStorage?.getTree(BookmarkRoot.Mobile.id)?.children
            bookmarks?.forEach { bookmarksStorage.deleteNode(it.guid) }
        }
    }

    @Test
    fun noBookmarkItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyEmptyBookmarksList()
        }
    }

    @Test
    fun verifyBookmarkButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            verifyAddBookmarkButton()
            clickAddBookmarkButton()
        }
        browserScreen {
        }.openThreeDotMenu {
            verifyEditBookmarkButton()
        }
    }

    @Test
    fun addBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
            verifyBookmarkedURL(defaultWebPage.url)
            verifyBookmarkFavicon()
        }
    }

    // @Ignore("Intermittent failure on Nexus 6: https://github.com/mozilla-mobile/fenix/issues/8772")
    @Test
    fun createBookmarkFolderTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            mDevice.waitForIdle(waitingTimeShort)
            clickAddFolderButton()
            verifyKeyboardVisible()
            addNewFolderName(bookmarksFolderName)
            saveNewFolder()
            getInstrumentation().waitForIdleSync()
            verifyFolderTitle(bookmarksFolderName)
            verifyKeyboardHidden()
        }
    }

    @Test
    fun cancelCreateBookmarkFolderTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            mDevice.waitForIdle(waitingTimeShort)
            clickAddFolderButton()
            addNewFolderName(bookmarksFolderName)
            navigateUp()
            verifyKeyboardHidden()
        }
    }

    @Test
    fun editBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickEdit {
            verifyEditBookmarksView()
            verifyBookmarkNameEditBox()
            verifyBookmarkURLEditBox()
            verifyParentFolderSelector()
            changeBookmarkTitle(bookmarkTitle)
            changeBookmarkUrl(bookmarkUrl)
            saveEditBookmark()
            getInstrumentation().waitForIdleSync()
            verifyBookmarkTitle(bookmarkTitle)
            verifyBookmarkedURL(bookmarkUrl.toUri())
            verifyKeyboardHidden()
        }
    }

    @Test
    fun copyBookmarkURLTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickCopy {
            verifyCopySnackBarText()
        }
    }

    @Test
    fun threeDotMenuShareBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickShare {
            verifyShareOverlay()
            verifyShareTabFavicon()
            verifyShareTabTitle()
            verifyShareTabUrl()
        }
    }

    @Test
    fun openBookmarkInNewTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickOpenInNewTab {
            verifyPageContent(defaultWebPage.content)
        }.openHomeScreen {
            verifyOpenTabsHeader()
        }
    }

    @Test
    fun openBookmarkInPrivateTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickOpenInPrivateTab {
            verifyPageContent(defaultWebPage.content)
        }.openHomeScreen {
            verifyPrivateSessionHeader()
        }
    }

    @Test
    fun deleteBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickDelete {
            verifyDeleteSnackBarText()
            verifyUndoDeleteSnackBarButton()
        }
    }

    @Test
    fun undoDeleteBookmarkTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickDelete {
            verifyUndoDeleteSnackBarButton()
            clickUndoDeleteButton()
            verifySnackBarHidden()
            verifyBookmarkedURL(defaultWebPage.url)
        }
    }

    @Test
    fun multiSelectionToolbarItemsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
            longTapSelectItem(defaultWebPage.url)
        }

        multipleSelectionToolbar {
            verifyMultiSelectionCheckmark()
            verifyMultiSelectionCounter()
            verifyShareBookmarksButton()
            verifyCloseToolbarButton()
        }.closeToolbarReturnToBookmarks {
            verifyBookmarksMenuView()
        }
    }

    @Test
    fun openSelectionInNewTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openHomeScreen {
            closeTab()
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
            longTapSelectItem(defaultWebPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }

        multipleSelectionToolbar {
        }.clickOpenNewTab {
            verifyExistingTabList()
            verifyOpenTabsHeader()
        }
    }

    @Test
    fun openSelectionInPrivateTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
            longTapSelectItem(defaultWebPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }

        multipleSelectionToolbar {
        }.clickOpenPrivateTab {
            verifyExistingTabList()
            verifyPrivateSessionHeader()
        }
    }

    // @Ignore("Temp disable: Nexus 6 failures - issue: https://github.com/mozilla-mobile/fenix/issues/7417")
    @Test
    fun deleteMultipleSelectionTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        browserScreen {
            createBookmark(firstWebPage.url)
            createBookmark(secondWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
            longTapSelectItem(firstWebPage.url)
            longTapSelectItem(secondWebPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }

        multipleSelectionToolbar {
            clickMultiSelectionDelete()
        }

        bookmarksMenu {
            verifyEmptyBookmarksList()
        }
    }

    @Test
    fun multipleSelectionShareButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
            mDevice.waitForIdle(waitingTimeShort)
            longTapSelectItem(defaultWebPage.url)
        }

        multipleSelectionToolbar {
            clickShareBookmarksButton()
            verifyShareOverlay()
            verifyShareTabFavicon()
            verifyShareTabTitle()
            verifyShareTabUrl()
        }
    }

    // @Ignore("Temp disable: Nexus 6 failures - issue: https://github.com/mozilla-mobile/fenix/issues/7417")
    @Test
    fun multipleBookmarkDeletions() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            mDevice.waitForIdle(waitingTimeShort)
            createFolder("1")
            getInstrumentation().waitForIdleSync()
            createFolder("2")
            getInstrumentation().waitForIdleSync()
            createFolder("3")
            getInstrumentation().waitForIdleSync()
        }.openThreeDotMenu("1") {
        }.clickDelete {
        }.openThreeDotMenu("2") {
        }.clickDelete {
            verifyFolderTitle("3")
        }.goBack {
        }

        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyFolderTitle("3")
        }
    }

    @Test
    fun changeBookmarkParentFolderTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        browserScreen {
            createBookmark(defaultWebPage.url)
        }.openThreeDotMenu {
        }.openLibrary {
        }.openBookmarks {
        }.openThreeDotMenu {
        }.clickEdit {
            verifyEditBookmarksView()
            changeBookmarkTitle(bookmarkTitle)
            saveEditBookmark()
            createFolder(bookmarksFolderName)
        }.openThreeDotMenu(bookmarkTitle){
        }.clickEdit {
            clickParentFolderSelector()
            selectFolder(bookmarksFolderName)
            navigateUp()
            saveEditBookmark()
            selectFolder(bookmarksFolderName)
            verifyBookmarkedURL(defaultWebPage.url)
        }
    }

    @Test
    fun navigateBookmarksFolders() {
        homeScreen {
        }.openThreeDotMenu {
        }.openBookmarks {
            mDevice.waitForIdle(waitingTimeShort)
            createFolder("1")
            selectFolder("1")
            createFolder("2")
            selectFolder("2")
            verifyCurrentFolderTitle("2")
            navigateUp()
            verifyCurrentFolderTitle("1")
            mDevice.pressBack()
            verifyBookmarksMenuView()
        }
    }
}
