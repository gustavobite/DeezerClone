package com.fevziomurtekin.deezer.repository

import com.fevziomurtekin.deezer.core.data.ApiCallback
import com.fevziomurtekin.deezer.domain.local.DeezerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import com.fevziomurtekin.deezer.core.data.ApiResult
import com.fevziomurtekin.deezer.core.extensions.*
import com.fevziomurtekin.deezer.core.mapper
import com.fevziomurtekin.deezer.data.albumdetails.AlbumData
import com.fevziomurtekin.deezer.data.artist.ArtistData
import com.fevziomurtekin.deezer.data.artistdetails.ArtistRelatedData
import com.fevziomurtekin.deezer.data.genre.Data
import com.fevziomurtekin.deezer.data.search.SearchData
import com.fevziomurtekin.deezer.entities.SearchEntity
import com.fevziomurtekin.deezer.domain.network.DeezerClient
import com.fevziomurtekin.deezer.entities.AlbumEntity
import com.fevziomurtekin.deezer.entities.GenreEntity
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

class DeezerRepository @Inject constructor(
    private val deezerClient: DeezerClient,
    private val deezerDao: DeezerDao
) : ApiCallback(), DeezerRepositoryImpl {

    override suspend fun fetchGenreList() = flow {
        emit( ApiResult.Loading )
        localCall {
            deezerDao.getGenreList()
        }.let { localResult ->
            localResult.isSucces.letOnTrueOnSuspend {
                networkCall {
                    deezerClient.fetchGenreList()
                }.let { apiResult->
                    apiResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                        (apiResult.getResult() as? List<Data>)?.let {
                            localCall { deezerDao.insertGenreList(it.mapper()) }
                            emit(ApiResult.Success(it))
                        }
                    }
                }
            }.letOnFalseOnSuspend {
                val result = (localResult as? List<GenreEntity>)?.mapper()
                delay(1500)
                emit(ApiResult.Success(result?.toList()))
            }
        }
    }.flowOn(Dispatchers.IO)


    override fun fetchArtistList(genreID:String)
    = flow {
        emit(ApiResult.Loading)
        networkCall {
            deezerClient.fetchArtistList(genreID)
        }.let { apiResult ->
            apiResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                emit(ApiResult.Success((apiResult.getResult() as? List<ArtistData>?)))
            }.letOnFalseOnSuspend {
                delay(1500)
                emit(ApiResult.Error(Exception("Unexpected error.")))
            }
        }
    }.flowOn(Dispatchers.IO)



    override fun fetchArtistDetails(artistID:String
    ) = flow {
        emit( ApiResult.Loading )
        networkCall {
            deezerClient.fetchArtistDetails(artistID)
        }.let { apiResult ->
            apiResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                emit(apiResult)
            }.letOnTrueOnSuspend {
                /* fake call */
                delay(1500)
                emit(ApiResult.Error(Exception("Unexpected error.")))
            }
        }
    }.flowOn(Dispatchers.IO)


    override fun fetchArtistAlbums(artistID:String
    ) = flow {
        emit( ApiResult.Loading )

        networkCall {
            deezerClient.fetchArtistDetails(artistID)
        }.let { apiResult ->
            apiResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                emit(ApiResult.Success(apiResult.getResult() as List<AlbumData>))
            }.letOnFalseOnSuspend {
                /* fake call */
                delay(1500)
                emit(ApiResult.Error(Exception("Unexpected error.")))
            }
        }
    }.flowOn(Dispatchers.IO)



    override fun fetchArtistRelated(artistID:String
    ) = flow {
        emit( ApiResult.Loading )

        networkCall {
            deezerClient.fetchArtistRelated(artistID)
        }.let { apiResult ->
            apiResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                emit(ApiResult.Success(apiResult.getResult() as List<ArtistRelatedData>))
            }.letOnFalseOnSuspend {
                /* fake call */
                delay(1500)
                emit(ApiResult.Error(Exception("Unexpected error.")))
            }
        }
    }.flowOn(Dispatchers.IO)


    override fun fetchAlbumTracks(albumID:String
    ) = flow {
        emit( ApiResult.Loading )
        networkCall {
            deezerClient.fetchAlbumDetails(albumID)
        }.let { apiResult ->
            apiResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                val response = apiResult.getResult() as List<AlbumData>
                response.forEach { it.durationToTime() }
                emit(ApiResult.Success(response))
            }.letOnFalseOnSuspend {
                /* fake call */
                delay(1500)
                emit(ApiResult.Error(Exception("Unexpected error.")))
            }
        }
    }.flowOn(Dispatchers.IO)


    override fun fetchRecentSearch()= flow {
        localCall {
            deezerDao.getQueryList()
        }.let { localResult->
            localResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                emit(ApiResult.Success(localResult.data as List<SearchEntity>))
            }.letOnFalseOnSuspend {
                emit(ApiResult.Error(Exception("Unexpected error.")))
            }
        }
    }.flowOn(Dispatchers.IO)


    override suspend fun insertSearch(query: SearchEntity)= localCall {
        deezerDao.insertQuery(query)
    }


    override fun fetchSearch(query:String) = flow{
        emit(ApiResult.Loading)
        localCall {
            insertSearch(SearchEntity(q=query))
        }.data.isNotNull().letOnTrueOnSuspend {
            networkCall {
                deezerClient.fetchSearchAlbum(query)
            }.let { apiResult ->
                apiResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                    emit(ApiResult.Success(apiResult.getResult() as List<SearchData>))
                }.letOnFalseOnSuspend {
                    /* fake call */
                    delay(1500)
                    emit(ApiResult.Error(Exception("Unexpected error.")))
                }
            }
        }.letOnFalseOnSuspend {
            /* fake call */
            delay(1500)
            emit(ApiResult.Error(Exception("Unexpected error.")))
        }
    }.flowOn(Dispatchers.IO)


    override suspend fun insertFavoritesData(track:AlbumEntity?) = localCall {
        track?.let {
            deezerDao.insertTrack(it)
        }
    }


    override fun fetchFavorites()= flow {
        Timber.d(" --------- fetchFavorites ---------")
        localCall {
            deezerDao.getFavorites()
        }.let { localResult ->
            localResult.isSuccessAndNotNull().letOnTrueOnSuspend {
                emit(ApiResult.Success(localResult.data as List<AlbumEntity>))
            }.letOnFalseOnSuspend {
                /* fake call */
                delay(1500)
                emit(ApiResult.Error(Exception("Unexpected error.")))
            }
        }
    }.flowOn(Dispatchers.IO)

}