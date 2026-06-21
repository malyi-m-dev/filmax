package com.filmax.app.navigation

import androidx.lifecycle.ViewModel
import com.filmax.core.domain.auth.AuthRepository

class RootViewModel(
    auth: AuthRepository,
) : ViewModel() {
    val isAuthenticated = auth.isAuthenticated
}
