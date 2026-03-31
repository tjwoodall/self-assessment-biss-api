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

import api.connectors.httpparsers.StandardDownstreamHttpParser.*
import api.connectors.{BaseDownstreamConnector, DownstreamOutcome, DownstreamUri}
import api.models.downstream.IncomeSourceType
import config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import v3.retrieveBiss.downstreamUriBuilder.RetrieveBissDownstreamUriBuilder
import v3.retrieveBiss.model.request.{Def1_RetrieveBissRequestData, RetrieveBissRequestData}
import v3.retrieveBiss.model.response.{Def1_RetrieveBissResponse, RetrieveBissResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveBissConnector @Inject() (val http: HttpClientV2, val appConfig: AppConfig)(implicit ec: ExecutionContext)
    extends BaseDownstreamConnector {

  def retrieveBiss(
      request: RetrieveBissRequestData)(implicit hc: HeaderCarrier, correlationId: String): Future[DownstreamOutcome[RetrieveBissResponse]] = {

    import request.*
    val incomeSourceType: IncomeSourceType = typeOfBusiness.toIncomeSourceType(taxYear.year)

    request match {
      case def1: Def1_RetrieveBissRequestData =>
        import def1.*

        val uriBuilder: RetrieveBissDownstreamUriBuilder[Def1_RetrieveBissResponse] =
          RetrieveBissDownstreamUriBuilder.downstreamUriFor(taxYear)

        val (downstreamUri, queryParams): (DownstreamUri[Def1_RetrieveBissResponse], Seq[(String, String)]) =
          uriBuilder.buildUri(nino, businessId, incomeSourceType, taxYear)

        val response: Future[DownstreamOutcome[Def1_RetrieveBissResponse]] = get(downstreamUri, queryParams)

        response
    }
  }

}
