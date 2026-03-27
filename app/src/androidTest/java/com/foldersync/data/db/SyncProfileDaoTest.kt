package com.foldersync.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.foldersync.data.db.dao.SyncProfileDao
import com.foldersync.data.db.entity.SyncProfileEntity
import com.foldersync.domain.model.ConflictStrategy
import com.foldersync.domain.model.SyncDirection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncProfileDaoTest {

    private lateinit var database: FolderSyncDatabase
    private lateinit var dao: SyncProfileDao

    @Before
    fun setup() {
        // In-memory database — destroyed after each test. No leftover state.
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FolderSyncDatabase::class.java)
            .allowMainThreadQueries()   // OK for tests only!
            .build()
        dao = database.syncProfileDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // Helper to create a test profile without repeating all fields
    private fun testProfile(
        name: String = "Test Profile",
        remoteUrl: String = "https://cloud.example.com/remote.php/dav/files/user/",
    ) = SyncProfileEntity(
        name = name,
        localUri = "content://com.android.externalstorage/tree/primary%3ASync",
        remoteUrl = remoteUrl,
        credentialRef = "cred_test",
        direction = SyncDirection.UPLOAD,
        conflictStrategy = ConflictStrategy.LOCAL_WINS,
    )

    @Test
    fun insertAndRetrieveProfile() = runTest {
        val profile = testProfile(name = "My Nextcloud")
        val id = dao.insert(profile)

        val retrieved = dao.getById(id)
        assertNotNull(retrieved)
        assertEquals("My Nextcloud", retrieved!!.name)
        assertEquals(SyncDirection.UPLOAD, retrieved.direction)
    }

    @Test
    fun observeAllReturnsFlowOfProfiles() = runTest {
        dao.insert(testProfile(name = "Alpha"))
        dao.insert(testProfile(name = "Beta"))

        // .first() collects the first emission from the Flow
        val profiles = dao.observeAll().first()
        assertEquals(2, profiles.size)
        // Should be sorted by name ASC
        assertEquals("Alpha", profiles[0].name)
        assertEquals("Beta", profiles[1].name)
    }

    @Test
    fun updateProfile() = runTest {
        val id = dao.insert(testProfile(name = "Original"))
        val original = dao.getById(id)!!

        dao.update(original.copy(name = "Updated", enabled = true))

        val updated = dao.getById(id)
        assertEquals("Updated", updated!!.name)
        assertEquals(true, updated.enabled)
    }

    @Test
    fun deleteProfile() = runTest {
        val id = dao.insert(testProfile())
        assertNotNull(dao.getById(id))

        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun getEnabledProfiles() = runTest {
        dao.insert(testProfile(name = "Enabled").copy(enabled = true))
        dao.insert(testProfile(name = "Disabled").copy(enabled = false))

        val enabled = dao.getEnabled()
        assertEquals(1, enabled.size)
        assertEquals("Enabled", enabled[0].name)
    }
}