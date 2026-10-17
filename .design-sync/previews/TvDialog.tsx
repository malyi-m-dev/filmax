import { TvDialog } from "@filmax/tv-ui";

/** Подтверждение деструктива: фокус на главной кнопке. */
export const DeleteFolder = () => (
  <TvDialog
    title="Удалить папку «Посмотреть зимой»?"
    message="Закладки из неё пропадут. Сами фильмы останутся в каталоге."
    confirmLabel="Удалить"
  />
);

/** Поверх экрана: затемнение и фокус на «Отмена». */
export const OverScreen = () => (
  <div style={{ position: "relative", width: 640, height: 300, background: "#141414 url(https://picsum.photos/seed/filmax-1/640/300) center/cover" }}>
    <TvDialog
      overlay
      title="Выйти из аккаунта?"
      message="На этом телевизоре понадобится снова ввести код с kino.pub/device."
      confirmLabel="Выйти"
      focused="cancel"
    />
  </div>
);
