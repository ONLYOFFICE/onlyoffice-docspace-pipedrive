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

/* eslint-disable jsx-a11y/label-has-associated-control */
import React from "react";
import Info from "@assets/info.svg";

type HintProps = {
  children?: JSX.Element | JSX.Element[];
};

export const OnlyofficeHint: React.FC<HintProps> = ({ children }) => (
  <div className="p-3 bg-sky-100 w-full border rounded-lg border-blue-400 text-slate-800 text-sm dark:bg-pipedrive-color-dark-blue-100 dark:text-pipedrive-color-dark-neutral-1000">
    <div className="flex">
      <div className="p-2">
        <Info />
      </div>
      <div className="p-2">{children}</div>
    </div>
  </div>
);
