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

import AppExtensionsSDK, { Command } from "@pipedrive/app-extensions-sdk";
import { jwtDecode, JwtPayload } from "jwt-decode";

export class PipedriveToken {
  private sdk: AppExtensionsSDK;

  private token: { value: string; exp: number } | undefined;

  constructor(sdk: AppExtensionsSDK) {
    this.sdk = sdk;
  }

  public async getValue(): Promise<string> {
    const currentTime = Math.floor(Date.now() / 1000);

    if (!this.token || this.token.exp < currentTime + 60) {
      const response = await this.sdk?.execute(Command.GET_SIGNED_TOKEN);
      const payload = jwtDecode<JwtPayload>(response.token);

      this.token = {
        value: response.token,
        exp: payload.exp || 0,
      };
    }

    return this.token.value;
  }
}
