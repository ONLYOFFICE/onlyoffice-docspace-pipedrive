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

module.exports = {
    content: ["./src/**/*.{ts,tsx}"],
    darkMode: "class",
    theme: {
        extend: {
            width: {
                1500: "1500px",
                1400: "1400px",
                1300: "1300px",
                1200: "1200px",
                1100: "1100px",
                1000: "1000px",
                900: "900px",
                800: "800px",
                700: "700px",
                600: "600px",
                536: "536px",
                454: "454px",
                400: "400px",
                327: "327px",
                320: "320px",
                300: "300px",
                200: "200px",
                197: "197px",
                180: "180px"
            },
            minWidth: {
                xxs: "14rem",
            },
            maxWidth: {
                xxs: "14rem",
            },
            height: {
                1500: "1500px",
                1400: "1400px",
                1300: "1300px",
                1200: "1200px",
                1100: "1100px",
                1000: "1000px",
                900: "900px",
                800: "800px",
                700: "700px",
                600: "600px",
                536: "536px",
                454: "454px",
                400: "400px",
                327: "327px",
                300: "300px",
                200: "200px",
                197: "197px",
            },
            colors: {
                onlyoffice: "#0F4071",

                "onlyoffice-custom-ligth-subtitle": "#73767C",
                "pipedrive-color-light-neutral-100": "#f5f5f6",
                "pipedrive-color-light-neutral-200": "#ececed",
                "pipedrive-color-light-neutral-500": "#93949a",
                "pipedrive-color-light-neutral-700": "#65686f",
                "pipedrive-color-light-neutral-1000": "#21232c",
                "pipedrive-color-light-blue-200": "#e1eeff",
                "pipedrive-color-light-blue-600": "#2b74da",
                "pipedrive-color-light-blue-700": "#0d68c5",
                "pipedrive-color-light-red-600": "#d83c38",
                "pipedrive-color-light-red-700": "#c82627",
                "pipedrive-color-light-green-600": "#2d8647",
                "pipedrive-color-light-green-700": "#077838",
                "pipedrive-color-light-divider-strong": "rgba(33, 35, 44, .24)",

                "onlyoffice-custom-dark-subtitle": "#72747A",
                "pipedrive-color-dark-neutral-100": "#1e2029",
                "pipedrive-color-dark-neutral-200": "#2a2c35",
                "pipedrive-color-dark-neutral-500": "#686970",
                "pipedrive-color-dark-neutral-700": "#898b90",
                "pipedrive-color-dark-neutral-1000": "#e2e2e4",
                "pipedrive-color-dark-blue-100": "#001f4b",
                "pipedrive-color-dark-blue-200": "#012a60",
                "pipedrive-color-dark-blue-600": "#4073c8",
                "pipedrive-color-dark-blue-800": "#6e9dec",
                "pipedrive-color-dark-red-600": "#cc4543", 
                "pipedrive-color-dark-red-700": "#e0645e",
                "pipedrive-color-dark-green-600": "#3c824e",
                "pipedrive-color-dark-green-700": "#5d9867",
                "pipedrive-color-dark-divider-medium": "hsla(240, 4%, 89%, .15)",
                "pipedrive-color-dark-divider-strong": "hsla(240, 4%, 89%, .15)",

                "pipedrive-color-extra-light-rgba": "hsla(240, 4%, 89%, .06)",

                "smoke-light": "rgba(0, 0, 0, 0.4)",
            },
            screens: {
                wrap: { raw: "(max-width: 713px)" },
                scrollable: { raw: "(max-height: 550px)" },
                large: { raw: "(min-height: 730px) and (min-width: 900px)" },
                small: { raw: "(max-width: 452px)" },
                xsmall: { raw: "(max-width: 300px)" },
                xxsmall: { raw: "(max-width: 150px)" },
            },
            fontSize: {
                base: ["14.5px", "20px"],
                l: ["16px", "20px"],
                xl: ["21px", "32px"]
            }
        }
    },
    plugins: [],
};
