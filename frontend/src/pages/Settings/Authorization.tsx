import React, { useContext } from "react";
import { useTranslation } from "react-i18next";

import { OnlyofficeBackgroundError } from "@layouts/ErrorBackground";
import { OnlyofficeDocspaceLogin } from "@components/docspaceLogin";

import { AppContext } from "@context/AppContext";

import NotAvailable from "@assets/not-available.svg";

export type AuthorizationSettingProps = {
  showUserGuide(): void;
  onChangeSection(): void;
};

export const AuthorizationSetting: React.FC<AuthorizationSettingProps> = ({
  showUserGuide,
  onChangeSection,
}) => {
  const { t } = useTranslation();
  const { user, settings, reloadAppContext } = useContext(AppContext);

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
        <OnlyofficeDocspaceLogin showUserGuide={showUserGuide} />
      )}
    </>
  );
};
