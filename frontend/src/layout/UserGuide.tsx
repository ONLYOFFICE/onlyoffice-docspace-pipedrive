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

import React, { useState } from "react";

import { GuideCard } from "@components/guide-card";
import { ButtonColor, OnlyofficeButton } from "@components/button";
import { useTranslation } from "react-i18next";

const PAGES = [
  {
    title: "user-guide.page.1.title",
    image: "user-guide/create-rooms.png",
    steps: [
      "user-guide.page.1.step.1",
      "user-guide.page.1.step.2",
      "user-guide.page.1.step.3",
    ],
  },
  {
    title: "user-guide.page.2.title",
    image: "user-guide/access-rights.png",
    steps: ["user-guide.page.2.step.1", "user-guide.page.2.step.2"],
  },
  {
    title: "user-guide.page.3.title",
    image: "user-guide/create-documents.png",
    steps: [
      "user-guide.page.3.step.1",
      "user-guide.page.3.step.2",
      "user-guide.page.3.step.3",
      "user-guide.page.3.step.4",
    ],
  },
];

export type UserGuideProps = {
  onClose(): void;
};

export const UserGuide: React.FC<UserGuideProps> = ({ onClose }) => {
  const { t } = useTranslation();

  const [index, setIndex] = useState(0);

  return (
    <div className="flex flex-col p-4 pt-[60px] max-w-[952px]">
      <GuideCard
        title={t(PAGES[index].title)}
        image={PAGES[index].image}
        steps={PAGES[index].steps.map((step) => t(step))}
        progress={((index + 1) / PAGES.length) * 100}
      />
      <div className="flex flex-row justify-end gap-2 mt-2">
        {index === 0 && (
          <OnlyofficeButton text={t("button.skip", "Skip")} onClick={onClose} />
        )}
        {index !== 0 && (
          <OnlyofficeButton
            text={t("button.back", "Back")}
            onClick={() => {
              if (index > 0) {
                setIndex(index - 1);
              }
            }}
          />
        )}
        {index !== PAGES.length - 1 && (
          <OnlyofficeButton
            text={t("button.next", "Next")}
            onClick={() => {
              if (index < PAGES.length - 1) {
                setIndex(index + 1);
              }
            }}
            color={ButtonColor.PRIMARY}
          />
        )}
        {index === PAGES.length - 1 && (
          <OnlyofficeButton
            text={t("button.finish", "Finish")}
            onClick={onClose}
            color={ButtonColor.PRIMARY}
          />
        )}
      </div>
      {PAGES.map((page) => (
        <div key={page.title}>
          <img src={page.image} alt={page.title} className="hidden" />
        </div>
      ))}
    </div>
  );
};
