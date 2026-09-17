package com.tbtechs.folioreader.data.vocabulary

import com.tbtechs.folioreader.data.db.dao.UserWordDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserWordRepositoryImpl(
    userWordDao: UserWordDao,
    ioDispatcher: CoroutineDispatcher
) : UserWordRepository(userWordDao, ioDispatcher) {

    @Inject
    constructor(userWordDao: UserWordDao) : this(userWordDao, Dispatchers.IO)
}
