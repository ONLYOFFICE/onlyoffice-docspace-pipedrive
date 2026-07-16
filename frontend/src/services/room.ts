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

import { RoomResponse } from "src/types/room";
import { PipedriveToken } from "@context/PipedriveToken";

export const getRoom = async (
  pipedriveToken: PipedriveToken,
  dealId: number,
) => {
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

  const response = await client<RoomResponse>({
    method: "GET",
    url: `/api/v1/room/${dealId}`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    timeout: 10000,
  });

  return response.data;
};

export const postRoom = async (
  pipedriveToken: PipedriveToken,
  dealId: number,
  roomId: string,
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });

  const response = await client<RoomResponse>({
    method: "POST",
    url: `/api/v1/room/${dealId}`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    data: {
      roomId,
    },
    timeout: 30000,
  });

  return response.data;
};

export const requestAccessToRoom = async (
  pipedriveToken: PipedriveToken,
  dealId: number,
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });

  const response = await client<RoomResponse>({
    method: "POST",
    url: `/api/v1/room/${dealId}/request-access`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    timeout: 15000,
  });

  return response.data;
};

export const deleteRoom = async (
  pipedriveToken: PipedriveToken,
  dealId: number,
) => {
  const token = await pipedriveToken.getValue();
  const client = axios.create({ baseURL: process.env.BACKEND_URL });

  await client<RoomResponse>({
    method: "DELETE",
    url: `/api/v1/room/${dealId}`,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    timeout: 10000,
  });
};
