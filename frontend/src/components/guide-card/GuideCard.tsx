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

import React from "react";

type GuideCard = {
  title?: string;
  image?: string;
  steps?: Array<string>;
  progress?: number;
};

export const GuideCard: React.FC<GuideCard> = ({
  title,
  image,
  steps,
  progress,
}) => (
  <div className="flex flex-row">
    <div className="flex flex-col flex-1 gap-6">
      <div>
        <img src={image} alt={title} />
      </div>
      <div>
        <div className="w-full bg-pipedrive-color-light-neutral-200 dark:bg-pipedrive-color-dark-neutral-200 rounded-full h-1">
          <div
            className="bg-pipedrive-color-light-green-600 dark:bg-pipedrive-color-dark-green-600 h-1 rounded-full"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>
    </div>
    <div className="flex flex-col flex-1 pl-6 gap-4">
      <div className="text-lg font-semibold">{title}</div>
      <ul className="list-disc pl-6 text-sm">
        {steps?.map((step) => (
          <li key={step}>{step}</li>
        ))}
      </ul>
    </div>
  </div>
);
