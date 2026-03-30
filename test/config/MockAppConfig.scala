/*
 * Copyright 2025 HM Revenue & Customs
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
 */

package config

import cats.data.Validated
import org.scalamock.handlers.{CallHandler, CallHandler0}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.Configuration
import routing.Version

trait MockAppConfig extends TestSuite with MockFactory {

  implicit val mockAppConfig: AppConfig = mock[AppConfig]

  object MockedAppConfig {

    // IFS Config
    def ifsBaseUrl: CallHandler[String] = (() => mockAppConfig.ifsBaseUrl).expects()

    def ifsToken: CallHandler[String] = (() => mockAppConfig.ifsToken).expects()

    def ifsEnvironment: CallHandler[String] = (() => mockAppConfig.ifsEnv).expects()

    def ifsEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.ifsEnvironmentHeaders).expects()

    // HIP Config
    def hipBaseUrl: CallHandler[String] = (() => mockAppConfig.hipBaseUrl: String).expects()

    def hipEnv: CallHandler[String] = (() => mockAppConfig.hipEnv: String).expects()

    def hipClientId: CallHandler[String] = (() => mockAppConfig.hipClientId: String).expects()

    def hipClientSecret: CallHandler[String] = (() => mockAppConfig.hipClientSecret).expects()

    def hipEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.hipEnvironmentHeaders: Option[Seq[String]]).expects()

    // API Config
    def mtdIdBaseUrl: CallHandler[String] = (() => mockAppConfig.mtdIdBaseUrl).expects()

    def featureSwitches: CallHandler[Configuration] = (() => mockAppConfig.featureSwitches: Configuration).expects()

    def apiGatewayContext: CallHandler[String] = (() => mockAppConfig.apiGatewayContext: String).expects()

    def apiStatus(version: Version): CallHandler[String] = (mockAppConfig.apiStatus(_: Version)).expects(version)

    def endpointsEnabled(version: String): CallHandler[Boolean]  = (mockAppConfig.safeEndpointsEnabled(_: String)).expects(version)
    def endpointsEnabled(version: Version): CallHandler[Boolean] = (mockAppConfig.endpointsEnabled(_: Version)).expects(version)

    def deprecationFor(version: Version): CallHandler[Validated[String, Deprecation]] = (mockAppConfig.deprecationFor(_: Version)).expects(version)

    def apiDocumentationUrl(): CallHandler[String] = (() => mockAppConfig.apiDocumentationUrl: String).expects()

    def apiVersionReleasedInProduction(version: String): CallHandler[Boolean] =
      (mockAppConfig.apiVersionReleasedInProduction: String => Boolean).expects(version)

    def endpointReleasedInProduction(version: String, key: String): CallHandler[Boolean] =
      (mockAppConfig.endpointReleasedInProduction: (String, String) => Boolean).expects(version, key)

    def confidenceLevelConfig: CallHandler0[ConfidenceLevelConfig] =
      (() => mockAppConfig.confidenceLevelConfig).expects()

    def confidenceLevelCheckEnabled: CallHandler[ConfidenceLevelConfig] =
      (() => mockAppConfig.confidenceLevelConfig: ConfidenceLevelConfig).expects()

    def endpointAllowsSupportingAgents(endpointName: String): CallHandler[Boolean] =
      (mockAppConfig.endpointAllowsSupportingAgents(_: String)).expects(endpointName)

  }

}
