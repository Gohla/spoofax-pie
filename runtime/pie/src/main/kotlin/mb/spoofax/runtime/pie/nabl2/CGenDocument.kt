package mb.spoofax.runtime.pie.nabl2

import com.google.inject.Inject
import mb.log.Logger
import mb.nabl2.spoofax.analysis.ImmutableInitialResult
import mb.nabl2.spoofax.analysis.ImmutableUnitResult
import mb.pie.runtime.core.ExecContext
import mb.pie.runtime.core.TaskDef
import mb.pie.runtime.core.stamp.FileStampers
import mb.spoofax.runtime.impl.stratego.StrategoRuntimeBuilder
import mb.spoofax.runtime.impl.stratego.primitive.ScopeGraphPrimitiveLibrary
import mb.spoofax.runtime.model.SpoofaxEx
import mb.vfs.path.PPath
import org.spoofax.interpreter.terms.IStrategoTerm
import java.io.Serializable

class CGenDocument
@Inject constructor(
  log: Logger,
  private val primitiveLibrary: ScopeGraphPrimitiveLibrary
) : TaskDef<CGenDocument.Input, ImmutableUnitResult?> {
  private val log: Logger = log.forContext(CGenDocument::class.java)

  companion object {
    const val id = "nabl2.CGenDocument"
  }

  data class Input(
    val globalConstraints: ImmutableInitialResult,
    val ast: IStrategoTerm,
    val file: PPath,
    val langSpecExt: String,
    val root: PPath
  ) : Serializable

  override val id = Companion.id
  override fun ExecContext.exec(input: Input): ImmutableUnitResult? {
    val (globalConstraints, ast, file, langSpecExt, root) = input
    val constraintGenerator = requireOutput(CompileStrategoCGen::class.java, CompileStrategoCGen.id, CompileStrategoCGen.Input(
      langSpecExt, root
    )) ?: return null
    val strategoRuntime = constraintGenerator.createSuitableRuntime(StrategoRuntimeBuilder(), primitiveLibrary)
    require(constraintGenerator.strategoCtree(), FileStampers.hash)
    return try {
      constraintGenerator.cgenDocument(globalConstraints, ast, file.toString(), strategoRuntime)
    } catch(e: SpoofaxEx) {
      log.error("Generating document constraints failed", e)
      null
    }
  }
}