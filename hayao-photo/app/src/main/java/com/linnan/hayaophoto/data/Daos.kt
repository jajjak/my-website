package com.linnan.hayaophoto.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY name ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM persons WHERE id = :id")
    fun observeById(id: Long): Flow<PersonEntity?>

    @Insert
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Delete
    suspend fun delete(person: PersonEntity)
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE personId = :personId ORDER BY createdAt DESC")
    fun observeByPerson(personId: Long): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT COUNT(*) FROM media_items WHERE personId = :personId")
    suspend fun countForPerson(personId: Long): Int

    @Insert
    suspend fun insert(item: MediaItemEntity): Long

    @Delete
    suspend fun delete(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
