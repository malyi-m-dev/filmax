package com.filmax.data.jellyfin

/** Ответы сервера в том виде, в каком их отдаёт Jellyfin 10.9+: PascalCase, лишние поля, пропуски. */
internal object JellyfinFixtures {

    const val SERIES_ID = "5e1e5e1e000000000000000000000001"
    const val MOVIE_ID = "30f1e000000000000000000000000002"
    const val EPISODE_S2E1 = "e2e10000000000000000000000000003"

    val SERIES = """
        {"Id":"$SERIES_ID","Name":"Тьма","Type":"Series","ProductionYear":2017,"Overview":"Винден",
         "CommunityRating":8.73,"Status":"Ended","ImageTags":{"Primary":"p1"},"BackdropImageTags":["b1"],
         "GenreItems":[{"Name":"Драма","Id":"9e000000000000000000000000000001"}],
         "People":[{"Name":"Луис Хофман","Type":"Actor"},{"Name":"Баран бо Одар","Type":"Director"}],
         "ProviderIds":{"Imdb":"tt5753856","Tmdb":"70523"},"UserData":{"IsFavorite":true,"Played":false},
         "SomethingNew":{"Nested":1}}
    """.trimIndent()

    val EPISODES = """
        {"Items":[
          {"Id":"special","Name":"Спецвыпуск","Type":"Episode","IndexNumber":1,"ParentIndexNumber":0},
          {"Id":"e1e1","Name":"Секреты","Type":"Episode","IndexNumber":1,"ParentIndexNumber":1,
           "RunTimeTicks":31800000000,"ImageTags":{"Primary":"s1"},
           "UserData":{"PlaybackPositionTicks":6000000000,"Played":false}},
          {"Id":"$EPISODE_S2E1","Name":"Начало и конец","Type":"Episode","IndexNumber":1,"ParentIndexNumber":2,
           "UserData":{"Played":true}}
        ],"TotalRecordCount":3}
    """.trimIndent()

    val MOVIE = """
        {"Id":"$MOVIE_ID","Name":"Брат","Type":"Movie","ProductionYear":1997,"RunTimeTicks":59400000000,
         "Width":1920,"Height":816,"ImageTags":{"Primary":"m1"},
         "MediaStreams":[
           {"Type":"Video","Index":0,"Codec":"h264","Width":1920,"Height":816},
           {"Type":"Audio","Index":1,"Codec":"ac3","Language":"rus","Title":"Оригинал","Channels":6},
           {"Type":"Audio","Index":2,"Codec":"aac","Language":"eng","Channels":2}
         ]}
    """.trimIndent()

    val HISTORY = """
        {"Items":[
          {"Id":"$EPISODE_S2E1","Type":"Episode","Name":"Начало и конец","SeriesId":"$SERIES_ID","SeriesName":"Тьма",
           "SeriesPrimaryImageTag":"p1","IndexNumber":1,"ParentIndexNumber":2,"RunTimeTicks":36000000000,
           "ImageTags":{"Primary":"s2"},
           "UserData":{"PlaybackPositionTicks":12000000000,"LastPlayedDate":"2024-05-01T18:30:12.0000000Z"}},
          {"Id":"other-episode","Type":"Episode","SeriesId":"$SERIES_ID","SeriesName":"Тьма","IndexNumber":9,
           "ParentIndexNumber":1,"UserData":{"Played":true,"LastPlayedDate":"2024-04-01T10:00:00.0000000Z"}},
          {"Id":"never","Type":"Movie","Name":"Не смотрели","UserData":{"Played":false}}
        ]}
    """.trimIndent()

    fun playbackInfo(directPlay: Boolean) = """
        {"MediaSources":[{"Id":"ms1","SupportsDirectPlay":$directPlay,"RunTimeTicks":59400000000,
          "TranscodingUrl":"/videos/$MOVIE_ID/master.m3u8?MediaSourceId=ms1&ApiKey=secret-token",
          "MediaStreams":[
            {"Type":"Video","Index":0,"Width":1920,"Height":816},
            {"Type":"Audio","Index":1,"Language":"rus","Title":"Дубляж","Channels":6},
            {"Type":"Subtitle","Index":3,"Language":"rus","DeliveryMethod":"External",
             "DeliveryUrl":"/Videos/$MOVIE_ID/ms1/Subtitles/3/0/Stream.vtt"},
            {"Type":"Subtitle","Index":4,"Language":"eng","DeliveryMethod":"Encode"}
          ]}],"PlaySessionId":"ps1"}
    """.trimIndent()
}
