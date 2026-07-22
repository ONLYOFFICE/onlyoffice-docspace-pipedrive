import React, { useContext, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Command, View } from "@pipedrive/app-extensions-sdk";

import { ButtonColor, OnlyofficeButton } from "@components/button";
import { OnlyofficeTitle } from "@components/title";
import { OnlyofficeBackgroundError } from "@layouts/ErrorBackground";

import { AppContext } from "@context/AppContext";

import {
  deleteDocspaceAccount,
  getDocspaceOAuthAuthorizeUrl,
  postDocspaceOAuthCallback,
} from "@services/user";

import Authorized from "@assets/authorized.svg";
import NotAvailable from "@assets/not-available.svg";
import Welcome from "@assets/welcome.svg";

import {
  DOCSPACE_OAUTH_CALLBACK_MESSAGE_TYPE,
  DocspaceOAuthCallbackMessage,
} from "../../types/docspace";

export type AuthorizationSettingProps = {
  showUserGuide(): void;
  onChangeSection(): void;
};

export const AuthorizationSetting: React.FC<AuthorizationSettingProps> = ({
  showUserGuide,
  onChangeSection,
}) => {
  const { t } = useTranslation();
  const { user, settings, setUser, sdk, pipedriveToken, reloadAppContext } =
    useContext(AppContext);

  const [deleting, setDeleting] = useState(false);
  const [connectingOAuth, setConnectingOAuth] = useState(false);
  const oauthPopupRef = useRef<Window | null>(null);

  const handleLogout = async () => {
    const { confirmed } = await sdk.execute(Command.SHOW_CONFIRMATION, {
      title: t("label.warning", "Warning"),
      description:
        t(
          "settings.authorization.deleting.confirm-message",
          "Are you sure you want to log out?",
        ) || "",
      okText: t("button.logout", "Log out"),
    });

    if (!confirmed) {
      return;
    }

    setDeleting(true);
    deleteDocspaceAccount(pipedriveToken)
      .then(async () => {
        if (user && settings) {
          setUser({ ...user, docspaceAccount: null });
        }
        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "settings.authorization.deleting.ok",
            "ONLYOFFICE DocSpace authorization has been successfully deleted",
          ),
        });
      })
      .catch(async () => {
        await sdk.execute(Command.SHOW_SNACKBAR, {
          message: t(
            "error.common",
            "Something went wrong. Please reload the app.",
          ),
        });
      })
      .finally(() => setDeleting(false));
  };

  const handleConnectOAuth = async () => {
    setConnectingOAuth(true);

    let authorizeUrl: string;
    try {
      authorizeUrl = await getDocspaceOAuthAuthorizeUrl(pipedriveToken);
    } catch {
      setConnectingOAuth(false);
      await sdk.execute(Command.SHOW_SNACKBAR, {
        message: t(
          "settings.authorization.saving.error",
          "Could not save ONLYOFFICE DocSpace authorization",
        ),
      });
      return;
    }

    oauthPopupRef.current = window.open(
      authorizeUrl,
      "docspace-oauth",
      "width=600,height=720",
    );

    if (!oauthPopupRef.current) {
      setConnectingOAuth(false);
    }
  };

  useEffect(() => {
    if (!connectingOAuth) {
      return undefined;
    }

    const handleMessage = (
      event: MessageEvent<DocspaceOAuthCallbackMessage>,
    ) => {
      if (event.origin !== window.location.origin) {
        return;
      }
      if (event.data?.type !== DOCSPACE_OAUTH_CALLBACK_MESSAGE_TYPE) {
        return;
      }

      const { code, state, error } = event.data;
      oauthPopupRef.current = null;

      if (error || !code || !state) {
        setConnectingOAuth(false);
        return;
      }

      postDocspaceOAuthCallback(pipedriveToken, code, state)
        .then(async () => {
          reloadAppContext();
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.authorization.saving.ok",
              "ONLYOFFICE DocSpace authorization has been successfully saved",
            ),
          });
          showUserGuide();
        })
        .catch(async () => {
          await sdk.execute(Command.SHOW_SNACKBAR, {
            message: t(
              "settings.authorization.saving.error",
              "Could not save ONLYOFFICE DocSpace authorization",
            ),
          });
        })
        .finally(() => setConnectingOAuth(false));
    };

    const popupClosedCheck = setInterval(() => {
      if (oauthPopupRef.current?.closed) {
        clearInterval(popupClosedCheck);
        setConnectingOAuth(false);
      }
    }, 500);

    window.addEventListener("message", handleMessage);

    return () => {
      window.removeEventListener("message", handleMessage);
      clearInterval(popupClosedCheck);
    };
  }, [
    connectingOAuth,
    pipedriveToken,
    reloadAppContext,
    sdk,
    showUserGuide,
    t,
  ]);

  return (
    <>
      {(!settings?.url || !settings?.apiKey) && (
        <OnlyofficeBackgroundError
          Icon={<NotAvailable />}
          title={t("background.error.title.not-available", "Not yet available")}
          subtitle={
            user?.isAdmin
              ? `${t(
                  "background.error.subtitle.docspace-connection",
                  "You are not connected to ONLYOFFICE DocSpace.",
                )} ${t(
                  "background.error.hint.admin.docspace-connection",
                  "Please go to the Connection Setting to configure ONLYOFFICE DocSpace app settings.",
                )}`
              : `${t(
                  "background.error.subtitle.plugin.not-active.message",
                  "ONLYOFFICE DocSpace App is not yet available.",
                )} ${t(
                  "background.error.subtitle.plugin.not-active.help",
                  "Please wait until a Pipedrive Administrator configures the app settings.",
                )}`
          }
          button={
            !user?.isAdmin
              ? {
                  text: t("button.reload", "Reload"),
                  onClick: () => reloadAppContext(),
                }
              : {
                  text: t("button.settings", "Go to Settings"),
                  onClick: onChangeSection,
                }
          }
        />
      )}
      {settings?.apiKey && !settings?.isApiKeyValid && (
        <OnlyofficeBackgroundError
          Icon={<NotAvailable />}
          title={t("background.error.title.not-available", "Not yet available")}
          subtitle={`${t(
            "background.error.title.docspace-invalid-api-key",
            "The ONLYOFFICE DocSpace API Key is invalid.",
          )} ${
            user?.isAdmin
              ? t(
                  "background.error.hint.admin.docspace-connection",
                  "Please go to the Connection Setting to configure ONLYOFFICE DocSpace app settings.",
                )
              : t(
                  "background.error.hint.docspace-connection",
                  "Please contact the administrator.",
                )
          }`}
          button={
            !user?.isAdmin
              ? {
                  text: t("button.reload", "Reload"),
                  onClick: () => reloadAppContext(),
                }
              : undefined
          }
        />
      )}
      {settings?.url && settings?.apiKey && settings?.isApiKeyValid && (
        <>
          <div className="flex flex-col items-start pl-5 pr-5 pt-5 pb-3">
            <div className="pb-2">
              <OnlyofficeTitle
                text={t(
                  "settings.authorization.title",
                  "Log into your connected ONLYOFFICE DocSpace to start using it within Pipedrive",
                )}
              />
            </div>
            {!user?.docspaceAccount && (
              <div className="pt-3 pb-2">
                {t(
                  "settings.authorization.subtitle.address",
                  "Your connected DocSpace address is",
                )}{" "}
                <span className="font-semibold text-pipedrive-color-light-green-600 dark:text-pipedrive-color-dark-green-600">
                  {settings.url}
                </span>
              </div>
            )}
          </div>
          {!user?.docspaceAccount && (
            <div className="flex justify-start items-center pb-3 ml-5">
              <OnlyofficeButton
                text={t(
                  "settings.authorization.oauth.connect",
                  "Connect to DocSpace",
                )}
                color={ButtonColor.PRIMARY}
                loading={connectingOAuth}
                onClick={handleConnectOAuth}
              />
            </div>
          )}
          {user?.docspaceAccount && (
            <>
              <div className="flex gap-3 mt-1 pb-2 pl-5 pr-5">
                <div>
                  <Authorized />
                </div>
                <div className="flex justify-center items-center">
                  <p>
                    {t(
                      "settings.authorization.status.authorized",
                      "You have successfully logged in to your ONLYOFFICE DocSpace account",
                    )}{" "}
                    <span className="font-semibold text-pipedrive-color-light-green-600 dark:text-pipedrive-color-dark-green-600">
                      {user.docspaceAccount.userName}
                    </span>
                  </p>
                </div>
              </div>
              <div className="flex justify-start items-center mt-4 ml-5">
                <OnlyofficeButton
                  text={t("button.logout", "Log out")}
                  color={ButtonColor.NEGATIVE}
                  loading={deleting}
                  onClick={handleLogout}
                />
              </div>
              <div className="pt-4">
                <OnlyofficeBackgroundError
                  Icon={<Welcome />}
                  title={t(
                    "settings.authorization.welcome.title",
                    "Welcome to DocSpace!",
                  )}
                  options={[
                    t(
                      "settings.authorization.welcome.option1",
                      "Create rooms to work on deal documents",
                    ),
                    t(
                      "settings.authorization.welcome.option2",
                      "Edit and collaborate on docs, sheets, slides, forms, PDFs",
                    ),
                    t(
                      "settings.authorization.welcome.option3",
                      "Store your deal data securely in one single place",
                    ),
                  ]}
                  button={{
                    text: t("button.deals", "Go to Deals"),
                    onClick: async () => {
                      await sdk.execute(Command.REDIRECT_TO, {
                        view: View.DEALS,
                      });
                    },
                  }}
                  link={{
                    text: t(
                      "settings.authorization.welcome.open-guide",
                      "Open Guide",
                    ),
                    onClick: showUserGuide,
                  }}
                />
              </div>
            </>
          )}
        </>
      )}
    </>
  );
};
