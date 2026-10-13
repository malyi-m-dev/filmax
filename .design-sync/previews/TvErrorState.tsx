import { TvErrorState } from "@filmax/tv-ui";

/** Экран не загрузился, повтор возможен — кнопка сразу в фокусе. */
export const WithRetry = () => (
  <div style={{ width: 760, height: 320 }}>
    <TvErrorState
      title="Не удалось загрузить"
      message="Сервер kino.pub не ответил. Проверьте подключение к интернету и попробуйте ещё раз — сохранённые данные покажем, как только появится сеть."
      retryLabel="Повторить"
    />
  </div>
);

/** Ошибка, из которой повтор не выводит: без кнопки. */
export const WithoutRetry = () => (
  <div style={{ width: 760, height: 220 }}>
    <TvErrorState
      title="Нужна подписка"
      message="Этот фильм доступен с активной подпиской kino.pub. Продлить её можно на сайте или в приложении на телефоне."
    />
  </div>
);
