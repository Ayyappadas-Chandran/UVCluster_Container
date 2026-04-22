package com.suprajit.uvcluster

import com.suprajit.uvcluster.ui.viewModel.CarViewModel

object MyViewModelProvider {
    // Backing field for your ViewModel
    lateinit var instance: CarViewModel

    fun init(viewModel: CarViewModel) {
        instance = viewModel
    }
}

