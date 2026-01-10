package com.echo.feature.home.presentation

import app.cash.turbine.test
import com.echo.core.datastore.preferences.SessionData
import com.echo.core.datastore.preferences.SessionPreferences
import com.echo.feature.home.data.model.ActivityItem
import com.echo.feature.home.data.model.Friend
import com.echo.feature.home.data.model.Friendship
import com.echo.feature.home.data.model.ListeningUser
import com.echo.feature.home.data.model.PendingRequests
import com.echo.feature.home.data.model.SearchUserResult
import com.echo.feature.home.data.model.SocialOverview
import com.echo.feature.home.data.model.UserBasic
import com.echo.feature.home.data.repository.SocialRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var socialRepository: SocialRepository
    private lateinit var sessionPreferences: SessionPreferences
    private lateinit var viewModel: SocialViewModel

    private val testSession = SessionData(
        serverId = "server1",
        userId = "user1",
        username = "testuser",
        accessToken = "token",
        refreshToken = "refresh",
        expiresAt = System.currentTimeMillis() + 3600000
    )

    private val testFriends = listOf(
        Friend(
            id = "friend1",
            username = "friend_one",
            name = "Friend One",
            avatarUrl = "https://example.com/avatar1.jpg",
            isPublicProfile = true,
            friendshipId = "fs1",
            friendsSince = "2024-01-01"
        ),
        Friend(
            id = "friend2",
            username = "friend_two",
            name = "Friend Two",
            avatarUrl = null,
            isPublicProfile = false,
            friendshipId = "fs2",
            friendsSince = "2024-01-10"
        )
    )

    private val testPendingRequests = PendingRequests(
        received = listOf(
            Friend(
                id = "pending1",
                username = "pending_user",
                name = "Pending User",
                avatarUrl = null,
                isPublicProfile = true,
                friendshipId = "pfs1",
                friendsSince = "2024-01-15"
            )
        ),
        sent = emptyList(),
        count = 1
    )

    private val testListeningUsers = listOf(
        ListeningUser(
            id = "friend1",
            username = "friend_one",
            name = "Friend One",
            avatarUrl = "https://example.com/avatar1.jpg",
            isPlaying = true,
            currentTrack = null,
            updatedAt = "2024-01-15T10:30:00Z"
        )
    )

    private val testActivity = listOf(
        ActivityItem(
            id = "activity1",
            user = UserBasic("friend1", "friend_one", "Friend One", null),
            actionType = "played",
            targetType = "album",
            targetId = "album1",
            targetName = "Test Album",
            targetExtra = "Test Artist",
            targetCoverUrl = "https://example.com/cover.jpg",
            targetAlbumId = "album1",
            createdAt = "2024-01-15T10:00:00Z"
        )
    )

    private val testOverview = SocialOverview(
        friends = testFriends,
        pendingRequests = testPendingRequests,
        listeningNow = testListeningUsers,
        recentActivity = testActivity
    )

    private val testSearchResults = listOf(
        SearchUserResult(
            id = "user2",
            username = "searchuser",
            name = "Search User",
            avatarUrl = null,
            friendshipStatus = null
        ),
        SearchUserResult(
            id = "user3",
            username = "existingfriend",
            name = "Existing Friend",
            avatarUrl = null,
            friendshipStatus = "accepted"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        socialRepository = mockk(relaxed = true)
        sessionPreferences = mockk(relaxed = true)

        every { sessionPreferences.session } returns MutableStateFlow(testSession)
        coEvery { socialRepository.connectToListeningStream(any(), any()) } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows loading`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)

        // When
        viewModel = SocialViewModel(socialRepository, sessionPreferences)

        // Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `loadSocialData updates state with overview on success`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)

        // When
        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(testFriends, state.friends)
            assertEquals(testPendingRequests, state.pendingRequests)
            assertEquals(testListeningUsers, state.listeningNow)
            assertEquals(testActivity, state.recentActivity)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadSocialData sets error on failure`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns
            Result.failure(Exception("Network error"))

        // When
        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Network error", state.error)
        }
    }

    @Test
    fun `selectTab updates selected tab`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.selectTab(SocialTab.ACTIVITY)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(SocialTab.ACTIVITY, state.selectedTab)
        }
    }

    @Test
    fun `onSearchQueryChange updates query and triggers search for long queries`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.searchUsers("test", any()) } returns Result.success(testSearchResults)

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - query less than 2 chars
        viewModel.onSearchQueryChange("t")

        // Then - should not trigger search
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("t", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
        }

        // When - query >= 2 chars
        viewModel.onSearchQueryChange("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should trigger search
        coVerify { socialRepository.searchUsers("test", limit = 20) }
    }

    @Test
    fun `searchUsers updates results on success`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.searchUsers("search", any()) } returns Result.success(testSearchResults)

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onSearchQueryChange("search")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testSearchResults, state.searchResults)
            assertFalse(state.isSearching)
        }
    }

    @Test
    fun `sendFriendRequest updates user status to pending`() = runTest {
        // Given
        val friendship = Friendship(
            id = "fs_new",
            requesterId = "user1",
            addresseeId = "user2",
            status = "pending",
            createdAt = "2024-01-15",
            updatedAt = "2024-01-15"
        )
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.searchUsers(any(), any()) } returns Result.success(testSearchResults)
        coEvery { socialRepository.sendFriendRequest("user2") } returns Result.success(friendship)
        coEvery { socialRepository.getPendingRequests() } returns Result.success(testPendingRequests)

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Search first to populate results
        viewModel.onSearchQueryChange("search")
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.sendFriendRequest("user2")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { socialRepository.sendFriendRequest("user2") }
        viewModel.uiState.test {
            val state = awaitItem()
            val updatedUser = state.searchResults.find { it.id == "user2" }
            assertEquals("pending", updatedUser?.friendshipStatus)
        }
    }

    @Test
    fun `sendFriendRequest sets error on failure`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.sendFriendRequest("user2") } returns
            Result.failure(Exception("Request failed"))

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.sendFriendRequest("user2")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Request failed", state.error)
        }
    }

    @Test
    fun `acceptFriendRequest reloads social data on success`() = runTest {
        // Given
        val acceptedFriendship = Friendship(
            id = "pfs1",
            requesterId = "pending1",
            addresseeId = "user1",
            status = "accepted",
            createdAt = "2024-01-15",
            updatedAt = "2024-01-16"
        )
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.acceptFriendRequest("pfs1") } returns Result.success(acceptedFriendship)

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.acceptFriendRequest("pfs1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { socialRepository.acceptFriendRequest("pfs1") }
        coVerify(exactly = 2) { socialRepository.getSocialOverview() } // init + after accept
    }

    @Test
    fun `rejectFriendRequest removes from pending list on success`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.removeFriendship("pfs1") } returns Result.success(Unit)
        coEvery { socialRepository.getPendingRequests() } returns Result.success(
            PendingRequests(emptyList(), emptyList(), 0)
        )

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.rejectFriendRequest("pfs1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { socialRepository.removeFriendship("pfs1") }
        coVerify { socialRepository.getPendingRequests() }
    }

    @Test
    fun `removeFriend removes friend from list on success`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.removeFriendship("fs1") } returns Result.success(Unit)

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial friends
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.friends.size)
        }

        // When
        viewModel.removeFriend("fs1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.friends.size)
            assertFalse(state.friends.any { it.friendshipId == "fs1" })
        }
    }

    @Test
    fun `removeFriend sets error on failure`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.removeFriendship("fs1") } returns
            Result.failure(Exception("Cannot remove"))

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.removeFriend("fs1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Cannot remove", state.error)
            // Friend should still be in list
            assertEquals(2, state.friends.size)
        }
    }

    @Test
    fun `clearError removes error message`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns
            Result.failure(Exception("Test error"))

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error exists
        viewModel.uiState.test {
            val stateWithError = awaitItem()
            assertEquals("Test error", stateWithError.error)
        }

        // When
        viewModel.clearError()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    @Test
    fun `initial selected tab is FRIENDS`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)

        // When
        viewModel = SocialViewModel(socialRepository, sessionPreferences)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(SocialTab.FRIENDS, state.selectedTab)
        }
    }

    @Test
    fun `empty search query clears results`() = runTest {
        // Given
        coEvery { socialRepository.getSocialOverview() } returns Result.success(testOverview)
        coEvery { socialRepository.searchUsers(any(), any()) } returns Result.success(testSearchResults)

        viewModel = SocialViewModel(socialRepository, sessionPreferences)
        testDispatcher.scheduler.advanceUntilIdle()

        // First, do a search
        viewModel.onSearchQueryChange("test")
        testDispatcher.scheduler.advanceUntilIdle()

        // When - clear query
        viewModel.onSearchQueryChange("")

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchQuery)
            assertTrue(state.searchResults.isEmpty())
        }
    }
}
