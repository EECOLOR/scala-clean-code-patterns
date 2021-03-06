package processes.monadTransformers.vanillaScala

import play.api.mvc.AnyContent
import play.api.mvc.Request
import processes.PatchAssignment
import processes.Services

class Plain(services:Services) extends PatchAssignment with Machinery {

  def handlePatchRequest(id: String, request: Request[AnyContent]) = {
    val patchProgram =
      for {
        json <- services.parseJson(request) |>
          HttpResult.fromOption(results.badRequest)
        profile <- services.jsonToProfile(json) |>
          HttpResult.fromJsResult(results.unprocessableEntity )
        existingProfile <- services.getProfileById(id) |>
          HttpResult.fromFutureOption(results.notFound(id))
        mergedProfile <- services.mergeProfile(existingProfile, profile) |>
          HttpResult.fromAny
        _ <- services.updateProfile(id, mergedProfile) |>
          HttpResult.fromAnyFuture
      } yield results.noContent

    patchProgram.run
      .map(_.merge)
  }
}