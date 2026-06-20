package com.filmax.app.navigation

import androidx.lifecycle.ViewModel
import com.filmax.core.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    auth: AuthRepository,
) : ViewModel() {
    val isAuthenticated = auth.isAuthenticated
}
