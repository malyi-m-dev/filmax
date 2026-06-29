package com.filmax.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.tv.navigation.FilmaxTvNavGraph

class TvMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilmaxTvTheme {
                FilmaxTvNavGraph()
            }
        }
    }
}
