package mb.spoofax.pie

import com.google.inject.*
import com.google.inject.multibindings.MapBinder
import mb.pie.api.TaskDef
import mb.pie.taskdefs.guice.TaskDefsModule
import mb.pie.vfs.path.PathSrv
import mb.pie.vfs.path.PathSrvImpl
import mb.spoofax.pie.config.ParseLangSpecCfg
import mb.spoofax.pie.config.ParseWorkspaceCfg
import mb.spoofax.pie.constraint.*
import mb.spoofax.pie.esv.ESVToStylingRules
import mb.spoofax.pie.jsglr.JSGLRParse
import mb.spoofax.pie.legacy.*
import mb.spoofax.pie.nabl2.CompileCGen
import mb.spoofax.pie.nabl2.NaBL2ToStrategoCGen
import mb.spoofax.pie.sdf3.SDF3ToJSGLRParseTable
import mb.spoofax.pie.sdf3.SDF3ToStrategoSignatures
import mb.spoofax.pie.stratego.CompileStratego
import mb.spoofax.pie.style.SpoofaxStyle

open class SpoofaxPieTaskDefsModule : TaskDefsModule() {
  override fun Binder.bindTaskDefs(taskDefsBinder: MapBinder<String, TaskDef<*, *>>) {
    // Config
    bindTaskDef<ParseLangSpecCfg>(taskDefsBinder, ParseLangSpecCfg.id)
    bindTaskDef<ParseWorkspaceCfg>(taskDefsBinder, ParseWorkspaceCfg.id)

    // Runtime
    // Spoofax styling
    bindTaskDef<SpoofaxStyle>(taskDefsBinder, SpoofaxStyle.id)
    // JSGLR
    bindTaskDef<JSGLRParse>(taskDefsBinder, JSGLRParse.id)
    // Constraint generator
    bindTaskDef<CGenGlobal>(taskDefsBinder, CGenGlobal.id)
    bindTaskDef<CGenDocument>(taskDefsBinder, CGenDocument.id)
    // Constraint solver
    bindTaskDef<CSolveGlobal>(taskDefsBinder, CSolveGlobal.id)
    bindTaskDef<CSolveDocument>(taskDefsBinder, CSolveDocument.id)
    bindTaskDef<CSolveFinal>(taskDefsBinder, CSolveFinal.id)

    // Meta-language
    // ESV
    bindTaskDef<ESVToStylingRules>(taskDefsBinder, ESVToStylingRules.id)
    // Stratego
    bindTaskDef<CompileStratego>(taskDefsBinder, CompileStratego.id)
    // SDF3
    bindTaskDef<SDF3ToJSGLRParseTable>(taskDefsBinder, SDF3ToJSGLRParseTable.id)
    bindTaskDef<SDF3ToStrategoSignatures>(taskDefsBinder, SDF3ToStrategoSignatures.id)
    // NaBL2
    bindTaskDef<NaBL2ToStrategoCGen>(taskDefsBinder, NaBL2ToStrategoCGen.id)
    bindTaskDef<CompileCGen>(taskDefsBinder, CompileCGen.id)

    // Legacy
    bindTaskDef<LegacyLoadLanguage>(taskDefsBinder, LegacyLoadLanguage.id)
    bindTaskDef<LegacyLoadProject>(taskDefsBinder, LegacyLoadProject.id)
    bindTaskDef<LegacyParse>(taskDefsBinder, LegacyParse.id)
    bindTaskDef<LegacyParseAll>(taskDefsBinder, LegacyParseAll.id)
    bindTaskDef<LegacyAnalyze>(taskDefsBinder, LegacyAnalyze.id)
    bindTaskDef<LegacyAnalyzeAll>(taskDefsBinder, LegacyAnalyzeAll.id)
    bindTaskDef<LegacyTransform>(taskDefsBinder, LegacyTransform.id)
    bindTaskDef<LegacyTransformAll>(taskDefsBinder, LegacyTransformAll.id)
    bindTaskDef<LegacyBuildProject>(taskDefsBinder, LegacyBuildProject.id)
    bindTaskDef<LegacyBuildLangSpec>(taskDefsBinder, LegacyBuildLangSpec.id)
    bindTaskDef<LegacyBuildOrLoadLanguage>(taskDefsBinder, LegacyBuildOrLoadLanguage.id)
    bindTaskDef<LegacyLanguageExtensions>(taskDefsBinder, LegacyLanguageExtensions.id)
    bindTaskDef<LegacyStyle>(taskDefsBinder, LegacyStyle.id)
  }
}

open class SpoofaxPieModule : AbstractModule() {
  override fun configure() {
    bind(SpoofaxPipeline::class.java).`in`(Singleton::class.java)
  }
}

open class PieVfsModule : AbstractModule() {
  override fun configure() {
    bind(PathSrv::class.java).to(PathSrvImpl::class.java).`in`(Singleton::class.java)
  }
}
