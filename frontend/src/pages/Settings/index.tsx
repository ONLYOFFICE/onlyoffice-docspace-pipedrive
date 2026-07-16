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

import React, { useContext, useState } from "react";
import { useTranslation } from "react-i18next";

import { AppContext } from "@context/AppContext";
import { UserGuide } from "@layouts/UserGuide";

import { AuthorizationSetting } from "./Authorization";
import { ConnectionSettings } from "./Connection";

type Section = {
  id: string;
  title: string;
  available: boolean;
};

const SettingsPage: React.FC = () => {
  const { t } = useTranslation();
  const { user } = useContext(AppContext);

  const [showUserGuide, setShowUserGuide] = useState(false);

  const sections: Array<Section> = [
    {
      id: "conntection",
      title: t("settings.connection.menu-item", "Connection"),
      available: true,
    },
    {
      id: "authorization",
      title: t("settings.authorization.menu-item", "Authorization"),
      available: true,
    },
  ];

  if (!user?.isAdmin) {
    sections[0].available = false;
  }

  const firstAvailableSection = sections.find((section) => section.available);

  const [selectedSection, setSelectedSection] = useState<string | undefined>(
    firstAvailableSection?.id,
  );

  return (
    <div className="w-screen h-screen">
      {!showUserGuide && (
        <div className="flex flex-row">
          <div className="min-w-52 border-r-2 p-1 dark:border-pipedrive-color-dark-divider-medium">
            {sections
              .filter((section) => section.available)
              .map((section) => (
                <div
                  key={section.id}
                  id={section.id}
                  tabIndex={0}
                  role="button"
                  className={`text-left border-spacing-7 px-10 py-2 m-1 rounded cursor-pointer ${
                    section.id === selectedSection
                      ? "font-medium text-pipedrive-color-light-blue-700 bg-pipedrive-color-light-blue-200 dark:text-pipedrive-color-dark-blue-800 dark:bg-pipedrive-color-dark-blue-200"
                      : "hover:bg-stone-200 dark:hover:bg-pipedrive-color-extra-light-rgba"
                  }`}
                  onClick={() => setSelectedSection(section.id)}
                  onKeyDown={(event: React.KeyboardEvent<HTMLInputElement>) => {
                    if (event.key === "Enter") setSelectedSection(section.id);
                  }}
                >
                  {section.title}
                </div>
              ))}
          </div>
          <div className="custom-scroll w-screen h-screen overflow-y-scroll overflow-x-hidden p-2">
            {selectedSection === "conntection" && (
              <ConnectionSettings
                onChangeSection={() => setSelectedSection("authorization")}
              />
            )}
            {selectedSection === "authorization" && (
              <AuthorizationSetting
                showUserGuide={() => setShowUserGuide(true)}
                onChangeSection={() => setSelectedSection("conntection")}
              />
            )}
          </div>
        </div>
      )}
      {showUserGuide && (
        <div className="flex w-full h-full justify-center">
          <UserGuide
            onClose={() => {
              setShowUserGuide(false);
            }}
          />
        </div>
      )}
    </div>
  );
};
export default SettingsPage;
