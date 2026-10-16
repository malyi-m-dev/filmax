package com.filmax.data.feedback.di

import com.filmax.core.domain.feedback.FeedbackPromptStore
import com.filmax.core.domain.feedback.FeedbackRepository
import com.filmax.data.feedback.FeedbackPromptStoreImpl
import com.filmax.data.feedback.FeedbackRepositoryImpl
import com.filmax.data.feedback.FirestoreFeedbackApi
import org.koin.dsl.module

/**
 * Отзывы. Id Firebase-проекта — не секрет (он и в google-services.json, и в любом запросе), но
 * приходит Koin-свойством: сборка без Firebase оставит его пустым, и отправка честно упадёт.
 */
val feedbackModule = module {
    single { FirestoreFeedbackApi(engine = get(), projectId = getProperty(FIREBASE_PROJECT_PROPERTY, "")) }
    single<FeedbackRepository> { FeedbackRepositoryImpl(api = get()) }
    single<FeedbackPromptStore> { FeedbackPromptStoreImpl(settings = get()) }
}

const val FIREBASE_PROJECT_PROPERTY = "firebaseProjectId"
