package mb.spoofax.eclipse.menu;

import mb.common.util.EnumSetView;
import mb.common.util.ListView;
import mb.spoofax.core.language.LanguageInstance;
import mb.spoofax.core.language.menu.MenuItem;
import mb.spoofax.core.language.transform.*;
import mb.spoofax.eclipse.EclipseIdentifiers;
import mb.spoofax.eclipse.EclipseLanguageComponent;
import mb.spoofax.eclipse.SpoofaxEclipseComponent;
import mb.spoofax.eclipse.SpoofaxPlugin;
import mb.spoofax.eclipse.pie.PieRunner;
import mb.spoofax.eclipse.resource.EclipseResourcePath;
import mb.spoofax.eclipse.transform.TransformUtil;
import mb.spoofax.eclipse.util.SelectionUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import java.util.ArrayList;
import java.util.Iterator;

import static mb.spoofax.core.language.transform.TransformExecutionType.ManualOnce;

public abstract class ResourceContextMenu extends MenuShared {
    private final PieRunner pieRunner;

    private final EclipseLanguageComponent languageComponent;


    public ResourceContextMenu(EclipseLanguageComponent languageComponent) {
        final SpoofaxEclipseComponent component = SpoofaxPlugin.getComponent();
        this.pieRunner = component.getPieRunner();

        this.languageComponent = languageComponent;
    }


    @Override protected IContributionItem[] getContributionItems() {
        final @Nullable ISelection simpleSelection = SelectionUtil.getActiveSelection();
        if(!(simpleSelection instanceof IStructuredSelection)) {
            return new IContributionItem[0];
        }
        final IStructuredSelection selection = (IStructuredSelection) simpleSelection;

        final LanguageInstance languageInstance = languageComponent.getLanguageInstance();
        final EclipseIdentifiers identifiers = languageComponent.getEclipseIdentifiers();
        final MenuManager langMenu = new MenuManager(languageInstance.getDisplayName());

        // Selections.
        final ArrayList<IProject> projects = SelectionUtil.toProjects(selection);
        final boolean hasProjects = !projects.isEmpty();
        final ArrayList<IContainer> containers = SelectionUtil.toContainers(selection);
        final boolean hasContainers = !containers.isEmpty();
        final ArrayList<IFile> files = SelectionUtil.toFiles(selection);
        // Remove non-language files.
        for(Iterator<IFile> it = files.iterator(); it.hasNext(); ) {
            final IFile file = it.next();
            final @Nullable String fileExtension = file.getFileExtension();
            if(fileExtension == null || !languageInstance.getFileExtensions().contains(fileExtension)) {
                it.remove();
            }
        }
        final boolean hasFiles = !files.isEmpty();

        // Add/remove nature.
        if(hasProjects) {
            boolean addNature = false;
            boolean removeNature = false;
            for(IProject project : projects) {
                try {
                    if(!project.hasNature(identifiers.getNature())) {
                        addNature = true;
                    } else {
                        removeNature = true;
                    }
                } catch(CoreException e) {
                    // Ignore
                }
            }
            if(addNature) {
                langMenu.add(command(identifiers.getAddNatureCommand()));
            }
            if(removeNature) {
                langMenu.add(command(identifiers.getRemoveAddNatureCommand()));
            }
        }

        // Observe/unobserve check tasks.
        if(hasFiles) {
            final ArrayList<IFile> observeFiles = new ArrayList<>();
            final ArrayList<IFile> unobserveFiles = new ArrayList<>();
            for(IFile file : files) {
                if(pieRunner.isCheckObserved(languageComponent, file)) {
                    unobserveFiles.add(file);
                } else {
                    observeFiles.add(file);
                }
            }
            if(!observeFiles.isEmpty() || !unobserveFiles.isEmpty()) {
                langMenu.add(new Separator());
            }
            if(!observeFiles.isEmpty()) {
                langMenu.add(command(identifiers.getObserveCommand()));
            }
            if(!unobserveFiles.isEmpty()) {
                langMenu.add(command(identifiers.getUnobserveCommand()));
            }
        }

        // Transformations.
        final String transformCommandId = identifiers.getTransformCommand();
        for(MenuItem menuItem : languageInstance.getResourceContextMenuItems()) {
            menuItem.accept(new EclipseMenuItemVisitor(langMenu) {
                @Override
                protected void transformAction(IContributionManager menu, String displayName, TransformRequest transformRequest) {
                    final EnumSetView<TransformSubjectType> supportedTypes = transformRequest.transformDef.getSupportedSubjectTypes();
                    final TransformExecutionType executionType = transformRequest.executionType;
                    final ListView<TransformInput> inputs;
                    if(executionType == ManualOnce) {
                        // Only manual one-shot transforms are supported.
                        if(hasProjects && supportedTypes.contains(TransformSubjectType.Project)) {
                            inputs = TransformUtil.inputs(projects.stream().map(EclipseResourcePath::new).map(TransformSubjects::project));
                        } else if(hasContainers && supportedTypes.contains(TransformSubjectType.Directory)) {
                            inputs = TransformUtil.inputs(containers.stream().map(EclipseResourcePath::new).map(TransformSubjects::directory));
                        } else if(hasFiles && supportedTypes.contains(TransformSubjectType.File)) {
                            inputs = TransformUtil.inputs(files.stream().map(EclipseResourcePath::new).map(TransformSubjects::file));
                        } else if(hasFiles && supportedTypes.contains(TransformSubjectType.Editor)) {
                            inputs = TransformUtil.inputs(files.stream().map(EclipseResourcePath::new).map(TransformSubjects::editor));
                        } else if(supportedTypes.contains(TransformSubjectType.None)) {
                            inputs = TransformUtil.input(TransformSubjects.none());
                        } else {
                            return;
                        }
                    } else {
                        // Other execution types are not supported.
                        return;
                    }
                    menu.add(transformCommand(transformCommandId, transformRequest, inputs, displayName));
                }
            });
        }

        return new IContributionItem[]{langMenu};
    }
}