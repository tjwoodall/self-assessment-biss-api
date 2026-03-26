/*
 * Copyright 2026 HM Revenue & Customs
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

import api.controllers.RequestContext
import api.models.outcomes.ResponseWrapper
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.*
import v3.retrieveBiss.downstreamErrorMapping.RetrieveBissDownstreamErrorMapping.errorMapFor
import v3.retrieveBiss.model.request.RetrieveBissRequestData
import v3.retrieveBiss.model.response.{Def1_RetrieveBissResponse, RetrieveBissResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveBissService @Inject() (connector: RetrieveBissConnector) extends BaseService {

  def retrieveBiss(
      request: RetrieveBissRequestData
  )(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[RetrieveBissResponse]] = {

    connector
      .retrieveBiss(request)
      .map(_.map { wrapper =>

        val model = wrapper.responseData

        val isDefaultNonTys =
          !request.taxYear.useTaxYearSpecificApi &&
            request.businessId.businessId == "XAIS12345678910"

        val transformed =
          if (request.taxYear.useTaxYearSpecificApi || isDefaultNonTys) {
            model
          } else {
            model match {
              case Def1_RetrieveBissResponse(total, profit, loss, _) =>
                Def1_RetrieveBissResponse(
                  total.copy(
                    additions = None,
                    deductions = None,
                    accountingAdjustments = None
                  ),
                  profit.copy(
                    adjusted = None
                  ),
                  loss,
                  outstandingBusinessIncome = None
                )
            }
          }

        ResponseWrapper(wrapper.correlationId, transformed)

      }.leftMap(mapDownstreamErrors(errorMapFor(request.taxYear).errorMap)))

  }

}
