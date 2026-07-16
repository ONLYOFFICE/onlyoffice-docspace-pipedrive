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

import axios from "axios";
import axiosRetry from "axios-retry";

import { SettingsResponse } from "src/types/settings";
import { PipedriveToken } from "@context/PipedriveToken";

export const getSettings = async (pipedriveToken: PipedriveToken) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });
  axiosRetry(client, {
    retries: 2,
    retryCondition: (error) =>
      axiosRetry.isNetworkOrIdempotentRequestError(error) ||
      (error.response?.status !== undefined && error.response.status >= 500),
    retryDelay: (count) => count * 50,
    shouldResetTimeout: true,
  });

  const response = await client<SettingsResponse>({
    method: "GET",
    url: `/api/v1/settings`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    timeout: 5000,
  });

  return response.data;
};

export const putSettings = async (
  pipedriveToken: PipedriveToken,
  url: string,
  apiKey: string,
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });

  const response = await client({
    method: "PUT",
    url: `/api/v1/settings`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    data: {
      url,
      apiKey,
    },
    timeout: 20000,
  });

  return response.data;
};

export const deleteSettings = async (pipedriveToken: PipedriveToken) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });

  const response = await client({
    method: "DELETE",
    url: `/api/v1/settings`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    timeout: 15000,
  });

  return response.data;
};

export const validateApiKey = async (pipedriveToken: PipedriveToken) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });

  const response = await client<SettingsResponse>({
    method: "POST",
    url: `/api/v1/settings/validate-api-key`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    timeout: 15000,
  });

  return response.data;
};
