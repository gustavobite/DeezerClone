package com.fevziomurtekin.deezer.ui.artist.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.viewModels
import com.fevziomurtekin.deezer.R
import com.fevziomurtekin.deezer.core.Env
import com.fevziomurtekin.deezer.core.data.ApiResult
import com.fevziomurtekin.deezer.core.extensions.UIExtensions
import com.fevziomurtekin.deezer.core.ui.DataBindingFragment
import com.fevziomurtekin.deezer.databinding.FragmentArtistsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_artists.*
import timber.log.Timber

@AndroidEntryPoint
class ArtistsFragment : DataBindingFragment(){

    private lateinit var binding:FragmentArtistsBinding
    @VisibleForTesting
    val viewModel: ArtistViewModel by viewModels()
    private var id:String = "0" //default value.


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = binding(inflater,R.layout.fragment_artists,container)
        return binding.root
    }

    override fun getSafeArgs() {
        arguments?.let {
            id = it.getString(Env.BUND_ID).let {s->
                if(s.isNullOrEmpty()) "0" else s
            }
        }
    }

    override fun initBinding() {
        binding.apply {
            lifecycleOwner = this@ArtistsFragment
            adapter = ArtistAdapter()
            vm = viewModel
        }
    }

    override fun setListeners() {}

    override fun observeLiveData() {
        viewModel.fetchResult(id)
        viewModel.result.observe(viewLifecycleOwner, {
            when(it){
                ApiResult.Loading->{ }
                is ApiResult.Error->{
                    UIExtensions.showSnackBar(this@ArtistsFragment.lv_main,this@ArtistsFragment.getString(R.string.unexpected_error))
                    Timber.d("result : error isSplash : false")
                }
                is ApiResult.Success->{
                    Timber.d("result : succes isSplash : false")
                }
            }
        })
    }
}