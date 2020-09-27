/*
 * Copyright (c) 2020 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.location.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.app.CoroutineTestRule
import com.duckduckgo.app.blockingObserve
import com.duckduckgo.app.global.db.AppDatabase
import com.duckduckgo.app.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

@ExperimentalCoroutinesApi
class LocationPermissionsRepositoryTest {

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var dao: LocationPermissionsDao
    private lateinit var repository: LocationPermissionsRepository

    private val domain = "domain.com"

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.locationPermissionsDao()
        repository = LocationPermissionsRepository(db.locationPermissionsDao(), coroutineRule.testDispatcherProvider)
    }

    @After
    fun after() {
        db.close()
    }

    @Test
    fun whenPermissionIsValidThenItIsStored() = coroutineRule.runBlocking {
        val permissionType = LocationPermissionType.ALLOW_ALWAYS
        val permission = LocationPermissionEntity(domain, permissionType)
        val permissionStored = repository.savePermission(domain, LocationPermissionType.ALLOW_ALWAYS)
        assertEquals(permission, permissionStored)
    }

    @Test
    fun whenGetAllPermissionsAsyncThenReturnLiveDataWithAllItemsFromDatabase() = coroutineRule.runBlocking {
        givenPermissionStored("example.com", "example2.com")
        val entities = repository.getLocationPermissionsAsync().blockingObserve()!!
        assertEquals(entities.size, 2)
    }

    @Test
    fun whenGetAllPermissionsSyncThenReturnListWithAllItemsFromDatabase() = coroutineRule.runBlocking {
        givenPermissionStored("example.com", "example2.com")
        val entities = repository.getLocationPermissionsSync()
        assertEquals(entities.size, 2)
    }

    @Test
    fun whenGetAllFireproofWebsitesThenReturnLiveDataWithAllItemsFromDatabase() = coroutineRule.runBlocking {
        givenPermissionStored("example.com", "example2.com")
        val entities = repository.getLocationPermissionsAsync().blockingObserve()!!
        assertEquals(entities.size, 2)
    }

    @Test
    fun whenDeletePermissionStoredThenItemRemovedFromDatabase() = coroutineRule.runBlocking {
        givenPermissionStored("example.com", "example2.com")

        assertEquals(dao.allPermissions().size, 2)

        repository.deletePermission("example.com")

        assertEquals(dao.allPermissions().size, 1)
    }

    @Test
    fun whenDeletePermissionNotStoredThenNotingIsRemovedFromDatabase() = coroutineRule.runBlocking {
        givenPermissionStored("example.com", "example2.com")

        assertEquals(dao.allPermissions().size, 2)

        repository.deletePermission("example3.com")

        assertEquals(dao.allPermissions().size, 2)
    }

    @Test
    fun whenRetrievingStoredPermissionThenItCanBeRetrieved() = coroutineRule.runBlocking {
        givenPermissionStored("example.com", "example2.com")

        val retrieved = repository.getDomainPermission("example2.com")!!

        assertEquals(retrieved.domain, "example2.com")
    }

    @Test
    fun whenRetrievingNotStoredPermissionThenNothingCanBeRetrieved() = coroutineRule.runBlocking {
        givenPermissionStored("example.com", "example2.com")

        val retrieved = repository.getDomainPermission("exampl3.com")

        assertNull(retrieved)
    }

    private fun givenPermissionStored(vararg domains: String) {
        domains.forEach {
            dao.insert(LocationPermissionEntity(domain = it, permission = LocationPermissionType.ALLOW_ALWAYS))
        }
    }
}
