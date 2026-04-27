package com.emilflach.lokcal.viewmodel

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.data.ExerciseTypeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseManageViewModelTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repo: ExerciseTypeRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        repo = ExerciseTypeRepository(Database(driver))
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun testStartEditingNullThenTypeFastSavesFullName() = runTest {
        // Simulates the UI flow: LaunchedEffect calls startEditing(null), then user types.
        // If user types before the startEditing coroutine runs, startEditing might
        // race with persist and reset state.
        val vm = ExerciseManageViewModel(repo, scope = this)

        vm.startEditing(null)  // mimics LaunchedEffect
        // Don't advance — user types immediately
        vm.setName("R")
        vm.setName("Ru")
        vm.setName("Running")
        vm.setKcal("300")
        advanceUntilIdle()

        val all = repo.getAll()
        assertEquals(1, all.size, "Expected 1 item but got ${all.size}: ${all.map { it.name }}")
        assertEquals("Running", all[0].name)
    }

    @Test
    fun testTypingOneCharAtATimeSavesFullName() = runTest {
        val vm = ExerciseManageViewModel(repo, scope = this)

        // Realistic UI: each keystroke fully persists before next one fires
        vm.setName("R")
        advanceUntilIdle()
        vm.setName("Ru")
        advanceUntilIdle()
        vm.setName("Run")
        advanceUntilIdle()
        vm.setName("Running")
        advanceUntilIdle()
        vm.setKcal("300")
        advanceUntilIdle()

        val all = repo.getAll()
        assertEquals(1, all.size, "Expected 1 item but got ${all.size}: ${all.map { it.name }}")
        assertEquals("Running", all[0].name, "Expected 'Running' but got '${all[0].name}' — only first letter saved?")
        assertEquals(300.0, all[0].kcal_per_hour)
    }

    @Test
    fun testRapidTypingCreatesOnlyOneItem() = runTest {
        val vm = ExerciseManageViewModel(repo, scope = this)

        vm.setName("R")
        vm.setName("Ru")
        vm.setName("Run")
        vm.setName("Running")
        vm.setKcal("300")
        advanceUntilIdle()

        val all = repo.getAll()
        assertEquals(1, all.size, "Expected 1 item but got ${all.size}: ${all.map { it.name }}")
        assertEquals("Running", all[0].name)
        assertEquals(300.0, all[0].kcal_per_hour)
    }

    @Test
    fun testNewItemGetsIdAndIsEditAfterSave() = runTest {
        val vm = ExerciseManageViewModel(repo, scope = this)

        vm.setName("Cycling")
        vm.setKcal("450")
        advanceUntilIdle()

        assertTrue(vm.edit.value.isEdit, "isEdit should be true after first save")
        assertNotNull(vm.edit.value.id, "id should be set after first save")
    }

    @Test
    fun testEditExistingItemDoesNotCreateDuplicate() = runTest {
        val id = repo.insert("Cycling", 400.0)
        val vm = ExerciseManageViewModel(repo, scope = this)
        advanceUntilIdle()

        vm.startEditing(id)
        advanceUntilIdle()

        vm.setName("Road Cycling")
        vm.setKcal("500")
        advanceUntilIdle()

        val all = repo.getAll()
        assertEquals(1, all.size, "Expected 1 item but got ${all.size}: ${all.map { it.name }}")
        assertEquals("Road Cycling", all[0].name)
        assertEquals(500.0, all[0].kcal_per_hour)
    }

    @Test
    fun testDeleteRemovesItem() = runTest {
        val id = repo.insert("Yoga", 250.0)
        val vm = ExerciseManageViewModel(repo, scope = this)
        advanceUntilIdle()

        vm.startEditing(id)
        advanceUntilIdle()

        var deleted = false
        vm.delete { deleted = true }
        advanceUntilIdle()

        assertTrue(deleted)
        assertEquals(0, repo.getAll().size)
    }
}
