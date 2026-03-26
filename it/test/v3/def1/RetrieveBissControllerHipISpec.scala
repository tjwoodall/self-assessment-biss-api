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

package v3.def1

import api.models.domain.TaxYear
import api.models.errors.*
import api.services.*
import play.api.http.HeaderNames.*
import play.api.http.MimeTypes
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.IntegrationBaseSpec
import v3.fixtures.RetrieveBissFixture

class RetrieveBissControllerHipISpec extends IntegrationBaseSpec with RetrieveBissFixture {

  "Calling the retrieve BISS endpoint" should {
    "return a valid response with status OK" when {

      "valid TYS request is made" in new TysTest("2025-26", "25-26", "01") {
        DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, OK, downstreamResponseJsonFull)

        val response: WSResponse = await(request.get())

        response.json shouldBe responseJsonFull
        response.status shouldBe OK
        response.header(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)
      }

      checkWith("self-employment", "self-employment")
      checkWith("uk-property-fhl", "fhl-property-uk")
      checkWith("uk-property", "uk-property")
      checkWith("foreign-property", "foreign-property")
      checkWith("foreign-property-fhl-eea", "fhl-property-eea")

      def checkWith(requestTypeOfBusiness: String, requestIncomeSourceType: String): Unit = {
        Seq(
          ("2023-24", "23-24"),
          ("2024-25", "24-25")
        ).foreach { case (mtdTaxYear, downstreamTaxYear) =>
          s"type of business is $requestTypeOfBusiness and tax year is $mtdTaxYear (TYS)" in new TysTest(
            mtdTaxYear,
            downstreamTaxYear,
            requestIncomeSourceType
          ) {
            override val typeOfBusiness: String   = requestTypeOfBusiness
            override val incomeSourceType: String = requestIncomeSourceType

            DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUrl, OK, downstreamResponseJsonMin)

            val response: WSResponse = await(request.get())

            response.json shouldBe responseJsonMin
            response.status shouldBe OK
            response.header(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)
          }
        }
      }
    }

    "return error according to spec" when {
      def validationErrorTest(requestNino: String,
                              requestTaxYear: String,
                              downstreamTaxYear: String,
                              requestBusinessId: String,
                              requestTypeOfBusiness: String,
                              expectedStatus: Int,
                              expectedBody: MtdError,
                              scenario: Option[String]): Unit = {
        s"validation fails with ${expectedBody.code} error ${scenario.getOrElse("")}" in new TysTest(
          requestTaxYear,
          downstreamTaxYear,
          "ignored"
        ) {
          override val taxYear: String        = requestTaxYear
          override val nino: String           = requestNino
          override val businessId: String     = requestBusinessId
          override val typeOfBusiness: String = requestTypeOfBusiness

          val response: WSResponse = await(request.get())

          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)
        }
      }

      val inputs = Seq(
        ("BadNino", "2025-26", "25-26", "XAIS12345678913", "self-employment", BAD_REQUEST, NinoFormatError, None),
        ("AA123456A", "2025", "25-26", "XAIS12345678913", "self-employment", BAD_REQUEST, TaxYearFormatError, None),
        ("AA123456A", "2016-17", "16-17", "XAIS12345678913", "self-employment", BAD_REQUEST, RuleTaxYearNotSupportedError, None),
        ("AA123456A", "2020-22", "20-21", "XAIS12345678913", "self-employment", BAD_REQUEST, RuleTaxYearRangeInvalidError, None),
        ("AA123456A", "2025-26", "25-26", "BadBusinessId", "self-employment", BAD_REQUEST, BusinessIdFormatError, None),
        ("AA123456A", "2025-26", "25-26", "XAIS12345678913", "uk-property-fhl", BAD_REQUEST, TypeOfBusinessFormatError, Some("for fhl")),
        ("AA123456A", "2025-26", "25-26", "XAIS12345678913", "foreign-property-fhl-eea", BAD_REQUEST, TypeOfBusinessFormatError, Some("for fhl eea")),
        ("AA123456A", "2025-26", "25-26", "XAIS12345678913", "not-business-type", BAD_REQUEST, TypeOfBusinessFormatError, None)
      )
      inputs.foreach(args => validationErrorTest.tupled(args))
    }

    "a downstream service error" when {
      def errorBody(`type`: String): String =
        s"""
           |{
           |  "origin": "HoD",
           |  "response": {
           |    "failures": [
           |      {
           |        "type": "${`type`}",
           |        "reason": "downstream message"
           |      }
           |    ]
           |  }
           |}
        """.stripMargin

      def serviceErrorTestTys(downstreamStatus: Int,
                              downstreamCode: String,
                              expectedStatus: Int,
                              expectedBody: MtdError,
                              mtdTaxYear: String,
                              downstreamTaxYear: String,
                              incomeSourceType: String): Unit = {
        s"downstream returns the $downstreamCode error and status $downstreamStatus for tax year $mtdTaxYear (TYS)" in new TysTest(
          mtdTaxYear,
          downstreamTaxYear,
          incomeSourceType
        ) {
          DownstreamStub.onError(DownstreamStub.GET, downstreamUrl, downstreamStatus, errorBody(downstreamCode))

          val response: WSResponse = await(request.get())

          response.json shouldBe Json.toJson(expectedBody)
          response.status shouldBe expectedStatus
          response.header(CONTENT_TYPE) shouldBe Some(MimeTypes.JSON)
        }
      }

      val api1871ErrorMap: Seq[(Int, String, Int, MtdError)] = Seq(
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
        (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_TYPE", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_INCOMESOURCE_ID", BAD_REQUEST, BusinessIdFormatError),
        (BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError),
        (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
        (UNPROCESSABLE_ENTITY, "INCOME_SUBMISSIONS_NOT_EXIST", BAD_REQUEST, RuleNoIncomeSubmissionsExist),
        (UNPROCESSABLE_ENTITY, "INVALID_ACCOUNTING_PERIOD", INTERNAL_SERVER_ERROR, InternalError),
        (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError),
        (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
      )

      val api1879ErrorMap: Seq[(Int, String, Int, MtdError)] = Seq(
        (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
        (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
        (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_INCOME_SOURCE_TYPE", INTERNAL_SERVER_ERROR, InternalError),
        (BAD_REQUEST, "INVALID_INCOME_SOURCE_ID", BAD_REQUEST, BusinessIdFormatError),
        (BAD_REQUEST, "UNMATCHED_STUB_ERROR", BAD_REQUEST, RuleIncorrectGovTestScenarioError),
        (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
        (UNPROCESSABLE_ENTITY, "INCOME_SUBMISSIONS_NOT_EXIST", BAD_REQUEST, RuleNoIncomeSubmissionsExist),
        (UNPROCESSABLE_ENTITY, "INVALID_ACCOUNTING_PERIOD", INTERNAL_SERVER_ERROR, InternalError),
        (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError),
        (UNPROCESSABLE_ENTITY, "REQUESTED_TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError),
        (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
      )

      Seq(
        ("2024-25", "24-25", api1871ErrorMap, "self-employment"),
        ("2025-26", "25-26", api1879ErrorMap, "01")
      ).foreach { case (mtdTaxYear, downstreamTaxYear, errorMap, incomeSourceType) =>
        errorMap.foreach { case (downstreamStatus, downstreamCode, expectedStatus, expectedBody) =>
          serviceErrorTestTys(
            downstreamStatus,
            downstreamCode,
            expectedStatus,
            expectedBody,
            mtdTaxYear,
            downstreamTaxYear,
            incomeSourceType
          )
        }
      }
    }
  }

  private trait Test {

    val nino: String           = "AA123456A"
    val businessId: String     = "XAIS12345678913"
    val typeOfBusiness: String = "self-employment"

    def incomeSourceType: String
    def taxYear: String
    def downstreamTaxYear: String
    def downstreamUrl: String

    def request: WSRequest = {
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.3.0+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

    def uri: String = s"/$nino/$typeOfBusiness/$taxYear/$businessId"
  }

  private class TysTest(mtdTaxYear: String, downstreamTysTaxYear: String, downstreamIncomeSourceType: String) extends Test {
    def incomeSourceType: String                     = downstreamIncomeSourceType
    def taxYear: String                              = mtdTaxYear
    private def taxYearMtd(taxYear: String): TaxYear = TaxYear.fromMtd(taxYear)
    def downstreamTaxYear: String                    = downstreamTysTaxYear

    def downstreamUrl: String = if (taxYearMtd(taxYear).year >= 2026) {
      s"/itsa/income-tax/v1/$downstreamTaxYear/income-sources/$nino/$businessId/$incomeSourceType/biss"
    } else {
      s"/itsa/income-tax/v1/income-sources/$downstreamTaxYear/$nino/$businessId/$incomeSourceType/biss"
    }

  }

}
