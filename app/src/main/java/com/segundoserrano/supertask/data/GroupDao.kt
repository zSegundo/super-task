package com.segundoserrano.supertask.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM `groups` ORDER BY position ASC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM `groups` ORDER BY position ASC")
    suspend fun getAllGroupsAsList(): List<Group>

    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): Group?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<Group>)

    @Update
    suspend fun updateGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)

    @Query("SELECT COUNT(*) FROM `groups`")
    suspend fun getGroupCount(): Int

    @Query("SELECT * FROM `groups` WHERE isPinned = 1 LIMIT 1")
    suspend fun getPinnedGroup(): Group?

    @Query("UPDATE `groups` SET isPinned = 0")
    suspend fun unpinAllGroups()

    @Query("UPDATE `groups` SET isPinned = 1 WHERE id = :groupId")
    suspend fun pinGroup(groupId: Long)
}