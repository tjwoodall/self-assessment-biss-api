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

package v3.retrieveBiss

import api.connectors.ConnectorSpec
import api.models.downstream.IncomeSourceType
import api.models.domain.*
import api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v3.retrieveBiss.def1.model.response.*
import v3.retrieveBiss.model.domain.TypeOfBusiness
import v3.retrieveBiss.model.request.{Def1_RetrieveBissRequestData, RetrieveBissRequestData}
import v3.retrieveBiss.model.response.{Def1_RetrieveBissResponse, RetrieveBissResponse}

import java.net.URL
import scala.concurrent.Future

class RetrieveBissConnectorSpec extends ConnectorSpec {

  private val nino: String                                    = "AA123456A"
  private val businessId: String                              = "businessId"
  private def taxYearMtd(taxYear: String): TaxYear            = TaxYear.fromMtd(taxYear)
  private def taxYearAsTysDownstream(taxYear: String): String = taxYearMtd(taxYear).asTysDownstream
  private def taxYearAsDownstream(taxYear: String): String    = taxYearMtd(taxYear).asDownstream

  // WLOG
  private val response: RetrieveBissResponse = Def1_RetrieveBissResponse(
    total = Total(100.00, 50.0, None, None, None),
    profit = Profit(0, 0, Some(35.00)),
    loss = Loss(100.0, 0.0),
    outstandingBusinessIncome = Some(35.00)
  )

  "RetrieveBissConnector" when {
    "retrieveBiss" should {
      "make a valid request to downstream as per the specification" when {
        Seq(
          ("2018-19", TypeOfBusiness.`self-employment`, IncomeSourceType.`self-employment`),
          ("2019-20", TypeOfBusiness.`uk-property`, IncomeSourceType.`uk-property`),
          ("2020-21", TypeOfBusiness.`uk-property-fhl`, IncomeSourceType.`fhl-property-uk`),
          ("2021-22", TypeOfBusiness.`foreign-property-fhl-eea`, IncomeSourceType.`fhl-property-eea`),
          ("2022-23", TypeOfBusiness.`foreign-property`, IncomeSourceType.`foreign-property`)
        ).foreach { case (taxYear, typeOfBusiness, incomeSourceType) =>
          s"type of business is $typeOfBusiness and tax year is $taxYear (Non TYS)" in new Test {
            val expectedUrl: URL = url"$baseUrl/itsa/income-tax/v1/income-sources/$nino/$incomeSourceType/${taxYearAsDownstream(taxYear)}/biss"

            val request: RetrieveBissRequestData =
              Def1_RetrieveBissRequestData(Nino(nino), typeOfBusiness, taxYearMtd(taxYear), BusinessId(businessId))

            val expected: Right[Nothing, ResponseWrapper[RetrieveBissResponse]] = Right(ResponseWrapper(correlationId, response))

            willGet(url = expectedUrl).returns(Future.successful(expected))

            await(connector.retrieveBiss(request)) shouldBe expected
          }
        }

        Seq(
          ("2023-24", TypeOfBusiness.`self-employment`, IncomeSourceType.`self-employment`),
          ("2024-25", TypeOfBusiness.`self-employment`, IncomeSourceType.`self-employment`),
          ("2025-26", TypeOfBusiness.`self-employment`, IncomeSourceType.`01`),
          ("2023-24", TypeOfBusiness.`uk-property`, IncomeSourceType.`uk-property`),
          ("2024-25", TypeOfBusiness.`uk-property`, IncomeSourceType.`uk-property`),
          ("2025-26", TypeOfBusiness.`uk-property`, IncomeSourceType.`02`),
          ("2023-24", TypeOfBusiness.`uk-property-fhl`, IncomeSourceType.`fhl-property-uk`),
          ("2024-25", TypeOfBusiness.`uk-property-fhl`, IncomeSourceType.`fhl-property-uk`),
          ("2023-24", TypeOfBusiness.`foreign-property-fhl-eea`, IncomeSourceType.`fhl-property-eea`),
          ("2024-25", TypeOfBusiness.`foreign-property-fhl-eea`, IncomeSourceType.`fhl-property-eea`),
          ("2023-24", TypeOfBusiness.`foreign-property`, IncomeSourceType.`foreign-property`),
          ("2024-25", TypeOfBusiness.`foreign-property`, IncomeSourceType.`foreign-property`),
          ("2025-26", TypeOfBusiness.`foreign-property`, IncomeSourceType.`15`)
        ).foreach { case (taxYear, typeOfBusiness, incomeSourceType) =>
          def urlPrefix(taxYear: String): String = {
            if (taxYearMtd(taxYear).year >= 2026)
              s"$baseUrl/itsa/income-tax/v1/${taxYearAsTysDownstream(taxYear)}/income-sources"
            else
              s"$baseUrl/itsa/income-tax/v1/income-sources/${taxYearAsTysDownstream(taxYear)}"
          }

          s"type of business is $typeOfBusiness and tax year is $taxYear (TYS)" in new Test {
            val expectedUrl: URL = url"${urlPrefix(taxYear)}/$nino/$businessId/$incomeSourceType/biss"

            val request: RetrieveBissRequestData =
              Def1_RetrieveBissRequestData(Nino(nino), typeOfBusiness, taxYearMtd(taxYear), BusinessId(businessId))

            val expected: Right[Nothing, ResponseWrapper[RetrieveBissResponse]] = Right(ResponseWrapper(correlationId, response))

            willGet(url = expectedUrl).returns(Future.successful(expected))

            await(connector.retrieveBiss(request)) shouldBe expected
          }
        }
      }

      "return an IllegalArgumentException" when {
        Seq(
          ("2025-26", TypeOfBusiness.`uk-property-fhl`, IncomeSourceType.`fhl-property-uk`),
          ("2026-27", TypeOfBusiness.`uk-property-fhl`, IncomeSourceType.`fhl-property-uk`),
          ("2025-26", TypeOfBusiness.`foreign-property-fhl-eea`, IncomeSourceType.`fhl-property-eea`),
          ("2026-27", TypeOfBusiness.`foreign-property-fhl-eea`, IncomeSourceType.`fhl-property-eea`)
        ).foreach { case (taxYear, typeOfBusiness, incomeSourceType) =>
          s"type of business is $typeOfBusiness and tax year is $taxYear (TYS)" in new Test {
            val request: RetrieveBissRequestData =
              Def1_RetrieveBissRequestData(Nino(nino), typeOfBusiness, taxYearMtd(taxYear), BusinessId(businessId))

            intercept[IllegalArgumentException] {
              await(connector.retrieveBiss(request))
            }.getMessage shouldBe s"Unsupported income source type: $incomeSourceType for tax year: ${taxYearMtd(taxYear).year}"
          }
        }
      }
    }
  }

  private trait Test extends IfsTest {
    self: ConnectorTest =>
    val connector: RetrieveBissConnector = new RetrieveBissConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

}
