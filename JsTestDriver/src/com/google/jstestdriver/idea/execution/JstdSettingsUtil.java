package com.google.jstestdriver.idea.execution;

import com.google.common.collect.Lists;
import com.google.jstestdriver.idea.config.JstdConfigFileType;
import com.google.jstestdriver.idea.execution.generator.JstdConfigGenerator;
import com.google.jstestdriver.idea.execution.settings.JstdConfigType;
import com.google.jstestdriver.idea.execution.settings.JstdRunSettings;
import com.google.jstestdriver.idea.execution.settings.TestType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Sergey Simonchik
 */
public class JstdSettingsUtil {

  private JstdSettingsUtil() {}

  @NotNull
  public static List<VirtualFile> collectJstdConfigs(@NotNull Project project, @NotNull JstdRunSettings runSettings) {
    TestType testType = runSettings.getTestType();
    List<VirtualFile> res = Collections.emptyList();
    if (testType == TestType.ALL_CONFIGS_IN_DIRECTORY) {
      VirtualFile virtualFile = VfsUtil.findFileByIoFile(new File(runSettings.getDirectory()), true);
      if (virtualFile != null) {
        res = collectJstdConfigFilesInDirectory(project, virtualFile);
      }
    } else {
      File configFile = extractConfigFile(project, runSettings);
      VirtualFile configVirtualFile = null;
      if (configFile != null) {
        configVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(configFile);
      }
      if (configVirtualFile != null) {
        res = Collections.singletonList(configVirtualFile);
      }
    }
    return res;
  }

  @Nullable
  private static File extractConfigFile(@NotNull Project project, @NotNull JstdRunSettings runSettings) {
    if (runSettings.getTestType() == TestType.CONFIG_FILE || runSettings.getConfigType() == JstdConfigType.FILE_PATH) {
      return new File(runSettings.getConfigFile());
    }
    try {
      return JstdConfigGenerator.INSTANCE.generateTempConfig(project, new File(runSettings.getJsFilePath()));
    } catch (IOException ignored) {
    }
    return null;
  }

  @NotNull
  public static List<VirtualFile> collectJstdConfigFilesInDirectory(@NotNull Project project, @NotNull VirtualFile directory) {
    GlobalSearchScope directorySearchScope = buildDirectorySearchScope(project, directory);
    if (directorySearchScope == null) {
      return Collections.emptyList();
    }
    Collection<VirtualFile> configs = FileTypeIndex.getFiles(JstdConfigFileType.INSTANCE, directorySearchScope);
    return Lists.newArrayList(configs);
  }

  public static boolean areJstdConfigFilesInDirectory(@NotNull Project project, @NotNull VirtualFile directory) {
    GlobalSearchScope directorySearchScope = buildDirectorySearchScope(project, directory);
    if (directorySearchScope == null) {
      return false;
    }
    return areJstdConfigFilesInScope(directorySearchScope);
  }

  private static boolean areJstdConfigFilesInScope(GlobalSearchScope scope) {
    final Ref<Boolean> jstdConfigFound = Ref.create(false);
    FileBasedIndex.getInstance().processValues(
      FileTypeIndex.NAME,
      JstdConfigFileType.INSTANCE,
      null,
      new FileBasedIndex.ValueProcessor<Void>() {
        @Override
        public boolean process(final VirtualFile file, final Void value) {
          jstdConfigFound.set(true);
          return false;
        }
      },
      scope
    );
    return jstdConfigFound.get();
  }

  @Nullable
  private static GlobalSearchScope buildDirectorySearchScope(@NotNull Project project, @NotNull VirtualFile directory) {
    final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(directory);
    if (module == null) {
      return null;
    }
    GlobalSearchScope directorySearchScope = GlobalSearchScopes.directoryScope(project, directory, true);
    return module.getModuleContentWithDependenciesScope().intersectWith(directorySearchScope);
  }

}
