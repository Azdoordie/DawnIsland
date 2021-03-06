package com.yanrou.dawnisland.entities

import androidx.room.*

@Entity(tableName = "cookie")
data class Cookie(
        @PrimaryKey
        @ColumnInfo(name = "userhash")
        val userHash: String,

        @ColumnInfo(name = "cookiename")
        val cookieName: String
)


@Dao
interface CookieDao {

    @Query("SELECT * FROM cookie")
    suspend fun getAll(): List<Cookie>

    @Query("SELECT * FROM cookie WHERE userhash==:userhash")
    suspend fun getCookieByUserHash(userhash: String): Cookie

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cookie: Cookie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cookieList: List<Cookie>)

    @Update
    suspend fun updateCookie(cookie: Cookie)

    @Delete
    suspend fun delete(cookie: Cookie)

    @Query("DELETE FROM cookie")
    fun nukeTable()
}