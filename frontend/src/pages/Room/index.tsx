/**
 *
 * (c) Copyright Ascensio System SIA 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import React, { useContext, useEffect, useRef, useState } from "react";
import i18next from "i18next";
import { useTranslation } from "react-i18next";
import { AxiosError } from "axios";
import { Color, Command, View } from "@pipedrive/app-extensions-sdk";
import {
  TFrameConfig,
  TFrameEvents,
} from "@onlyoffice/docspace-sdk-js/dist/types/types";

import { AppContext, AppErrorType } from "@context/AppContext";

import { deleteRoom, getRoom, postRoom } from "@services/room";
import { getDocspaceAccountToken } from "@services/user";
import { getCurrentURL, stripTrailingSlash } from "@utils/url";

import { OnlyofficeSpinner } from "@components/spinner";

import { getLocaleForDocspace } from "@utils/locale";
import {
  DropdownButtonColor,
  OnlyofficeDropdownButton,
} from "@components/dropdownButton";
import { DropdownButtonOptions } from "@components/dropdownButton/DropdownButton";
import { SDKInstance } from "@onlyoffice/docspace-sdk-js/dist/types/instance";
import { DocspaceUser } from "src/types/docspace";
import { RoomResponse } from "src/types/room";
import { ButtonColor, OnlyofficeButton } from "@components/button";
import { ErrorResponse } from "src/types/error";

const DOCSPACE_FRAME_ID = "docspace-frame";
const DOCSPACE_ROOM_TYPES = [
  {
    id: 6,
    name: "public",
  },
  {
    id: 2,
    name: "collaboration",
  },
  {
    id: 8,
    name: "vdr",
  },
  {
    id: 1,
    name: "form-filling-room",
  },
  {
    id: 5,
    name: "custom",
  },
];

let sdkLoaded = false;
let sdkLoading: Promise<void> | null = null;

function ensureSdk(url: string): Promise<void> {
  if (sdkLoaded) return Promise.resolve();
  if (sdkLoading) return sdkLoading;

  sdkLoading = new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = `${url}/static/scripts/sdk/2.2.0/api.js`;
    script.onload = () => {
      sdkLoaded = true;
      sdkLoading = null;
      resolve();
    };
    script.onerror = (e) => {
      sdkLoading = null;
      reject(e);
    };
    document.head.appendChild(script);
  });

  return sdkLoading;
}

const RoomPage: React.FC = () => {
  const { t } = useTranslation();
  const { parameters } = getCurrentURL();
  const { sdk, pipedriveToken, settings, setAppError } = useContext(AppContext);

  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [docspaceAuthorized, setDocspaceAuthorized] = useState(true);
  const [loadDocspace, setLoadDocspace] = useState(false);
  const [showDocspaceWindow, setShowDocspaceWindow] = useState(false);
  const [room, setRoom] = useState<RoomResponse | null>(null);

  const docspaceInstance = useRef<SDKInstance | null>(null);

  useEffect(() => {
    getRoom(pipedriveToken, Number(parameters.get("selectedIds")))
      .then(async (data) => {
        setRoom(data);
        setLoadDocspace(true);
      })
      .catch(async (e) => {
        if (e?.response?.status === 401) {
          setAppError(AppErrorType.TOKEN_ERROR);
        } else {
          setAppError(AppErrorType.COMMON_ERROR);
        }
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const onAppReady = () => {
    if (docspaceInstance.current && !room?.id) {
      docspaceInstance.current
        .getUserInfo()
        .then((data) => {
          const docspaceUser = data as DocspaceUser;

          if (
            !docspaceUser.isOwner &&
            !docspaceUser.isAdmin &&
            !docspaceUser.isRoomAdmin
          ) {
            docspaceInstance.current?.destroyFrame();
            setAppError(AppErrorType.DOCSPACE_ROOM_NOT_FOUND);
          }
        })
        .finally(() => {
          setLoading(false);
        });
    }
  };

  const onContentReady = () => {
    setShowDocspaceWindow(true);
    setLoading(false);
    sdk.execute(Command.RESIZE, { height: 680 });
  };

  const onAppError = (e: string | Event) => {
    // eslint-disable-next-line no-console
    console.error(e);

    setAppError(AppErrorType.DOCSPACE_UNREACHABLE);
    setLoading(false);
  };

  const onNoAccess = () => {
    setAppError(AppErrorType.DOCSPACE_ROOM_NO_ACCESS);
    setLoading(false);
  };

  const onNotFound = () => {
    deleteRoom(pipedriveToken, Number(parameters.get("selectedIds"))).catch(
      (e) => {
        if (e?.response?.status === 401) {
          setAppError(AppErrorType.TOKEN_ERROR);
        } else {
          setAppError(AppErrorType.COMMON_ERROR);
        }
      },
    );
    setRoom({ ...room, id: null } as RoomResponse);
    setShowDocspaceWindow(false);
    sdk.execute(Command.RESIZE, { height: 350 });
  };

  const saveRoom = (roomId: string) => {
    postRoom(pipedriveToken, Number(parameters.get("selectedIds")), roomId)
      .then((response) => {
        if (response.id && roomId !== response.id) {
          setRoom({ ...room, id: response.id } as RoomResponse);
        }
      })
      .catch((e) => {
        const data = e?.response?.data as ErrorResponse;
        if (
          e?.response?.status === 503 &&
          data?.cause === "DocspaceUrlNotFoundException"
        ) {
          setAppError(AppErrorType.PLUGIN_NOT_AVAILABLE);
          return;
        }

        if (
          e?.response?.status === 503 &&
          data?.cause === "DocspaceApiKeyNotFoundException"
        ) {
          setAppError(AppErrorType.PLUGIN_NOT_AVAILABLE);
          return;
        }

        if (
          e?.response?.status === 503 &&
          data?.cause === "DocspaceApiKeyInvalidException"
        ) {
          setAppError(AppErrorType.DOCSPACE_INVALID_API_KEY);
          return;
        }

        if (e?.response?.status === 401) {
          setAppError(AppErrorType.TOKEN_ERROR);
        } else {
          setAppError(AppErrorType.COMMON_ERROR);
        }
      });
  };

  const getCreateRoomDialogTitle = (roomType: string) => {
    const roomTypeName = DOCSPACE_ROOM_TYPES.find(
      (docspaceRoomType) => String(docspaceRoomType.id) === roomType,
    )?.name;

    if (!roomTypeName) {
      return "";
    }

    return `${t("button.create", "Create")} ${t(`docspace.room.type.${roomTypeName}`, roomTypeName)}`;
  };

  const getCreateRoomDialogDescription = (roomType: string) => {
    const roomTypeName = DOCSPACE_ROOM_TYPES.find(
      (docspaceRoomType) => String(docspaceRoomType.id) === roomType,
    )?.name;

    if (!roomTypeName) {
      return "";
    }

    return t(`docspace.room.type.${roomTypeName}.description`, roomTypeName);
  };

  const handleCreateRoom = async (roomType: string) => {
    if (docspaceInstance.current && room) {
      const { confirmed } = await sdk.execute(Command.SHOW_CONFIRMATION, {
        title: getCreateRoomDialogTitle(roomType),
        description: getCreateRoomDialogDescription(roomType),
        okText: t("button.create.room", "Create room"),
        okColor: Color.PRIMARY,
      });

      if (!confirmed) {
        return;
      }

      setCreating(true);

      docspaceInstance.current
        .createRoom(
          room.title,
          // @ts-expect-error Error in docspace-sdk-js types
          Number(roomType),
          undefined,
          ["Pipedrive Integration"],
        )
        .then(async (data) => {
          const docspaceRoom = data as { id: string; status: number };

          if (docspaceRoom.status && docspaceRoom.status !== 200) {
            let message = t(
              "room.creating.error",
              "Failed to create ONLYOFFICE DocSpace room!",
            );
            let link;

            if (docspaceRoom.status === 402) {
              message = t(
                "docspace.error.payment",
                "Creating this room is not possible since the limit is reached for the number of rooms included in your current plan.",
              );
              link = {
                url: `${stripTrailingSlash(settings?.url || "")}/portal-settings/payments/portal-payments`,
                label: t("docspace.link.payments", "Upgrade plan"),
              };
            }

            await sdk.execute(Command.SHOW_SNACKBAR, {
              message,
              link,
            });
            return;
          }

          docspaceInstance.current?.destroyFrame();
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "room.creating.ok",
              "ONLYOFFICE DocSpace room was successfully created!",
            ),
          });

          setRoom({ ...room, id: docspaceRoom.id } as RoomResponse);
          setLoading(true);
          saveRoom(docspaceRoom.id);
        })
        .finally(() => {
          setCreating(false);
        });
    }
  };

  const onUnsuccessLogin = () => {
    setDocspaceAuthorized(false);
    sdk.execute(Command.RESIZE, { height: 128 });
    setLoading(false);
  };

  const getToken = async () => {
    try {
      return await getDocspaceAccountToken(pipedriveToken);
    } catch (e) {
      const data = (e as AxiosError)?.response?.data as ErrorResponse;
      const isDocspaceOAuthError =
        data?.cause === "DocspaceOAuth2AuthorizationException";

      if (
        (e as AxiosError)?.response?.status === 401 &&
        !isDocspaceOAuthError
      ) {
        setAppError(AppErrorType.TOKEN_ERROR);
      } else {
        onUnsuccessLogin();
      }
      return "";
    }
  };

  const getRoomDocspaceConfig = () => {
    const config = {
      frameId: DOCSPACE_FRAME_ID,
      mode: "manager",
      width: "100%",
      height: "100%",
      id: room?.id,
      theme: sdk.userSettings.theme === "dark" ? "Dark" : "Base",
      showHeader: false,
      locale: getLocaleForDocspace(i18next.language),
      getToken,
      events: {
        onAppError,
        onNoAccess,
        onNotFound,
      } as TFrameEvents,
    } as unknown as TFrameConfig;

    if (room?.id && config.events) {
      config.events.onContentReady = onContentReady as (
        e?: Event | object | string,
      ) => void;
    }

    if (!room?.id && config.events) {
      config.events.onAppReady = onAppReady as (
        e?: Event | object | string,
      ) => void;
    }

    return config;
  };

  const getCreateRoomOptions = () =>
    DOCSPACE_ROOM_TYPES.reduce((createRoomOptions, roomType) => {
      createRoomOptions.push({
        id: String(roomType.id),
        label: t(
          `docspace.room.type.${roomType.name}.short-description`,
          roomType.name,
        ),
      });
      return createRoomOptions;
    }, new Array<DropdownButtonOptions>());

  useEffect(() => {
    if (!settings?.url) {
      return;
    }

    ensureSdk(settings.url)
      .then(() => {
        if (loadDocspace) {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const docspaceSDK = (window as any).DocSpace?.SDK;
          if (!docspaceSDK) return;

          const config = getRoomDocspaceConfig();
          config.src = settings?.url;

          docspaceInstance.current = docspaceSDK.init(config);
        }
      })
      .catch((e) =>
        // eslint-disable-next-line no-console
        console.error("[ONLYOFFICE AIChat] Failed to load DocSpace SDK", e),
      );
  }, [loadDocspace, settings?.url]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="w-full h-full flex flex-col">
      {loading && (
        <div className="h-full w-full flex justify-center items-center">
          <OnlyofficeSpinner />
        </div>
      )}
      {!loading && !docspaceAuthorized && (
        <div className="h-full flex flex-row custom-scroll overflow-y-scroll overflow-x-hidden">
          <div className="p-5">
            <div className="w-full pb-4">
              {t(
                "room.login.description",
                "Login to ONLYOFFICE DocSpace to easily collaborate on documents in this deal",
              )}
            </div>
            <OnlyofficeButton
              text={t("button.settings", "Go to Settings")}
              color={ButtonColor.PRIMARY}
              onClick={() =>
                sdk.execute(Command.REDIRECT_TO, { view: View.SETTINGS })
              }
            />
          </div>
        </div>
      )}
      {!loading && !room?.id && docspaceAuthorized && (
        <div className="h-full flex flex-row custom-scroll overflow-y-scroll overflow-x-hidden">
          <div className="p-5">
            <div className="w-full pb-4">
              {`${t(
                "room.create.description",
                "Create ONLYOFFICE DocSpace room to work with documents related to this deal.",
              )} `}
              <a
                className="font-semibold
                  text-pipedrive-color-light-blue-600
                  dark:text-pipedrive-color-dark-blue-600
                  hover:underline"
                target="_blank"
                href="https://helpcenter.onlyoffice.com/userguides/docspace-creating-rooms.aspx"
                rel="noreferrer"
              >
                {`${t("link.learn-more-room-types", "Learn more about room types.")}`}
              </a>
            </div>
            <OnlyofficeDropdownButton
              text={t("button.create.room", "Create room")}
              color={DropdownButtonColor.PRIMARY}
              loading={creating}
              options={getCreateRoomOptions()}
              onClick={handleCreateRoom}
            />
          </div>
        </div>
      )}
      {loadDocspace && settings?.url && docspaceAuthorized && (
        <div
          key={room?.id}
          className={`w-full h-full flex flex-col items-end
            ${!showDocspaceWindow ? "hidden" : ""}
          `}
        >
          <div id={DOCSPACE_FRAME_ID} />
          <div className="pr-4">
            <OnlyofficeButton
              text={t("button.open-in-docspace", "Open in DocSpace")}
              onClick={() => {
                const src = document
                  .getElementById(DOCSPACE_FRAME_ID)
                  ?.getAttribute("src");

                if (src) {
                  window.open(src, "_blank");
                }
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
};
export default RoomPage;
