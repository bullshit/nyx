/*
 * Copyright 2020 Mooltiverse
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
package com.mooltiverse.oss.nyx.git;

import java.io.File;
import java.util.Objects;

import com.mooltiverse.oss.nyx.git.util.FileSystemUtil;

/**
 * A scenario is predefined state of a Git repository that you can realize consistently
 * for test purposes.
 * 
 * Each scenario modelled here provides informations about the outcome of the applied scenario.
 */
public enum Scenario {
    /**
     * The scenario where the Git repository has been created but has no commits at all, not even
     * an initial commit. In this scenario the {@code HEAD} can't be resolved either.
     */
    FROM_SCRATCH( f -> Script.fromScratch(f) ),

    /**
     * The scenario where the Git repository has been created with just the initial commit.
     * The initial commit is not tagged.
     * This yields to a repository like:
     * 
     * <pre>
     *   * 1607ec8 (HEAD -> master) Initial commit
     * </pre>
     */
    INITIAL_COMMIT( f -> Script.fromScratch(f).andAddFiles()
        .andCommit("Initial commit")
    ),

    /**
     * The scenario where the Git repository has been created with just one commit after the initial commit.
     * The latest commit is tagged as {@code 0.1.0}.
     * This yields to a repository like:
     * 
     * <pre>
     *   * 4f4ae06 (HEAD -> master, tag: 0.1.0) Initial version
     *   * 1607ec8 Initial commit
     * </pre>
     */
    INITIAL_VERSION( f -> Script.fromScratch(f).andAddFiles()
        .andCommit("Initial commit")
        .andCommitWithTag("Initial version", "0.1.0", null)
    ),

    /**
     * The scenario where the Git repository has been created with a few tagged commits plus a couple of trailing
     * untagged commits.
     * The latest commit is tagged as {@code 0.4.0}.
     * This yields to a repository like:
     * 
     * <pre>
     *   * cb9423c (HEAD -> master) Untagged commit #2
     *   * ddb0b9f Untagged commit #1
     *   * 44944e7 (tag: 0.0.4) Commit lij
     *   * 92cd45c (tag: 0.0.3) Commit aem
     *   * 9709d14 (tag: 0.0.2) Commit gin
     *   * c996caa (tag: 0.0.1) Commit rfi
     *   * 2b0ce8c Initial commit
     * </pre>
     */
    ONE_BRANCH_SHORT( f -> Script.fromScratch(f).andAddFiles()
        .andCommit("Initial commit")
        .andCommitWithTag("0.0.1")
        .andCommitWithTag("0.0.2", "Annotated tag to commit 0.0.2")
        .andCommitWithTag("0.0.3")
        .andCommitWithTag("0.0.4", "Annotated tag to commit 0.0.4")
        .andCommit("Untagged commit #1")
        .andCommit("Untagged commit #2")
    ),

    /**
     * The scenario where the Git repository has been created with just one commit after the initial commit.
     * The latest tagged commit has overlapping tags, applied in reverse order, and is useful to test which
     * one is selected for bumping.
     * This yields to a repository like:
     * 
     * <pre>
     *   * b875514 (HEAD -> master) Untagged commit #2
     *   * 7c88def Untagged commit #1
     *   * 7d410cf (tag: 0.0.6, tag: 0.0.5, tag: 0.0.4) Commit smm
     *   * c99087c (tag: 0.0.3) Commit vkh
     *   * 7a24383 (tag: 0.0.2) Commit liu
     *   * 5b53015 (tag: 0.0.1) Commit tjk
     *   * 6018fc3 Initial commit
     * </pre>
     */
    ONE_BRANCH_WITH_OVERLAPPING_TAGS( f -> Script.fromScratch(f).andAddFiles()
        .andCommit("Initial commit")
        .andCommitWithTag("0.0.1")
        .andCommitWithTag("0.0.2", "Annotated tag to commit 0.0.2")
        .andCommitWithTag("0.0.3")
        .andCommitWithTag("0.0.6", "Annotated tag to commit 0.0.6")
        .andTag("0.0.5", null)
        .andTag("0.0.4", null)
        .andCommit("Untagged commit #1")
        .andCommit("Untagged commit #2")
    ),

    /**
     * The scenario where the Git repository has been created with two unmerged branches.
     * This yields to a repository like:
     * 
     * <pre>
     *   * 69b89ba (tag: 0.0.5-alpha.4, alpha) Commit lhj
     *   | * 4c18c69 (HEAD -> master, tag: 0.1.5) Commit exx
     *   * | e9a99f3 (tag: 0.0.5-alpha.3) Commit lib
     *   | * 20265d3 (tag: 0.1.4) Commit ftb
     *   * | f45f9d7 (tag: 0.0.5-alpha.2) Commit wys
     *   | * c4525f0 (tag: 0.1.3) Commit mui
     *   * | 0c63c69 (tag: 0.0.5-alpha.1) Commit khw
     *   | * c8d4839 (tag: 0.1.2) Commit olj
     *   | * c9885fa (tag: 0.1.1) Commit pfo
     *   | * 428ee21 (tag: 0.1.0) Commit unp
     *   |/  
     *   * ed65004 (tag: 0.0.4) Commit hvp
     *   * f82dae8 (tag: 0.0.3) Commit nvf
     *   * 4804d5c (tag: 0.0.2) Commit kwd
     *   * c4e58a1 (tag: 0.0.1) Commit wvi
     *   * 74bbba1 Initial commit
     * </pre>
     */
    TWO_BRANCH_SHORT_UNMERGED( f -> Script.fromScratch(f).andAddFiles()
        .andCommit("Initial commit")
        .andCommitWithTagInBranch("master", "0.0.1")
        .andCommitWithTagInBranch("master", "0.0.2", "Annotated tag to commit 0.0.2")
        .andCommitWithTagInBranch("master", "0.0.3")
        .andCommitWithTagInBranch("master", "0.0.4", "Annotated tag to commit 0.0.4")
        .andCommitWithTagInBranch("alpha", "0.0.5-alpha.1", "Annotated tag to commit 0.0.5-alpha.1")
        .andCommitWithTagInBranch("master", "0.1.0")
        .andCommitWithTagInBranch("alpha", "0.0.5-alpha.2")
        .andCommitWithTagInBranch("alpha", "0.0.5-alpha.3", "Annotated tag to commit 0.0.5-alpha.3")
        .andCommitWithTagInBranch("master", "0.1.1")
        .andCommitWithTagInBranch("alpha", "0.0.5-alpha.4")
        .andCommitWithTagInBranch("master", "0.1.2")
        .andCommitWithTagInBranch("master", "0.1.3")
        .andCommitWithTagInBranch("master", "0.1.4")
        .andCommitWithTagInBranch("master", "0.1.5")
    ),

    /**
     * The scenario where the Git repository has been created with two merged branches.
     * This yields to a repository like:
     * 
     * <pre>
     *   *   d372fcf (HEAD -> master, tag: 0.0.9) Merge alpha > master
     *   |\  
     *   * | b59e872 (tag: 0.0.8) Commit zmg
     *   | * 7044f5c (tag: 0.0.9-alpha.2, alpha) Commit jmy
     *   * | 89b80e6 (tag: 0.0.7) Commit voy
     *   | * e7398b5 (tag: 0.0.9-alpha.1) Commit xod
     *   |/  
     *   *   c77fae5 (tag: 0.0.6) Merge alpha > master
     *   |\  
     *   * | 7557baa (tag: 0.0.5) Commit gfq
     *   | * 666b6d3 (tag: 0.0.6-alpha.2) Commit dix
     *   | * 6f2f878 (tag: 0.0.6-alpha.1) Commit owf
     *   |/  
     *   * 6dab481 (tag: 0.0.4) Commit fle
     *   * fb5e77e (tag: 0.0.3) Commit xmi
     *   * 77dd131 (tag: 0.0.2) Commit vdw
     *   * 9c50fe4 (tag: 0.0.1) Commit aei
     *   * 29affd6 Initial commit
     * </pre>
     */
    TWO_BRANCH_SHORT_MERGED( f -> Script.fromScratch(f).andAddFiles()
        .andCommit("Initial commit")
        .andCommitWithTagInBranch("master", "0.0.1")
        .andCommitWithTagInBranch("master", "0.0.2", "Annotated tag to commit 0.0.2")
        .andCommitWithTagInBranch("master", "0.0.3")
        .andCommitWithTagInBranch("master", "0.0.4", "Annotated tag to commit 0.0.4")
        .andCommitWithTagInBranch("alpha", "0.0.6-alpha.1", "Annotated tag to commit 0.0.6-alpha.1")
        .andCommitWithTagInBranch("master", "0.0.5")
        .andCommitWithTagInBranch("alpha", "0.0.6-alpha.2")
        .andMergeIntoWithTag("master", "alpha", "Merge alpha > master", "0.0.6", null)
        .andMergeInto("alpha")
        .andCommitWithTagInBranch("alpha", "0.0.9-alpha.1", "Annotated tag to commit 0.0.9-alpha.1")
        .andCommitWithTagInBranch("master", "0.0.7")
        .andCommitWithTagInBranch("alpha", "0.0.9-alpha.2")
        .andCommitWithTagInBranch("master", "0.0.8")
        .andMergeIntoWithTag("master", "alpha", "Merge alpha > master", "0.0.9", null)
    ),

    /**
     * The scenario where the Git repository has been created with several branches
     * (all those contemplated in the {@link com.mooltiverse.oss.nyx.configuration.presets.Extended} preset).
     * <br>
     * Branches are:
     * 
     * <pre>
     * - master: a mainline branch with a few tagged commits
     * - main: a mainline branch with a a few non tagged commits
     * - integration: an integration branch with a few tagged commits
     * - development: an integration branch with a few non tagged commits
     * - alpha: a maturity branch with a few tagged commits
     * - beta: a maturity branch with a few tagged commits
     * - gamma: a maturity branch with a few non tagged commits
     * - v0.x: a maintenance branch with a few tagged commits
     * - v1.x: a maintenance branch with a few non tagged commits
     * - rel/0.x: a release branch with a few tagged commits
     * - rel/1.x: a release branch with a few non tagged commits
     * - feature/SSO: a feature branch with a few tagged commits
     * - feature/IN-12345: a feature branch with a few non tagged commits
     * - fix-98765: an hotfix branch with a few tagged commits
     * - somebranch: a generic branch for 'internal' contributions with a few tagged commits
     * - someotherbranch: a generic branch for 'internal' contributions with a few non tagged commits
     * </pre>
     * <br>
     * This yields to a repository like:
     * 
     * <pre>
     *   * 8f3dd93 (beta) Untagged commit #1 in branch beta
     *   * a297e00 (tag: 0.0.6-beta.2) Commit xde
     *   * d6b24a8 (tag: 0.0.6-beta.1) Commit xjl
     *   * 46ec855 (alpha) Untagged commit in branch alpha
     *   * c162a55 (tag: 0.0.6-alpha.2) Commit lxy
     *   * abbb668 (tag: 0.0.6-alpha.1) Commit djf
     *   | * e6781ca (gamma) Untagged commit #3 in branch gamma
     *   | * c2c5458 Untagged commit #2 in branch gamma
     *   | * 53ab7c1 Untagged commit #1 in branch gamma
     *   |/  
     *   | * 3c1bd3e (HEAD -> internal) Untagged commit #1 in branch internal
     *   | * 1180b5e (tag: 0.0.6-internal.1+timestamp.003) Commit gqe
     *   | * 5363e1c (tag: 0.0.6-internal.1+timestamp.002) Commit vfi
     *   | * c753c3c (tag: 0.0.6-internal.1+timestamp.001) Commit mto
     *   |/  
     *   | * af9a14c (tag: tag3, somebranch) Commit fho
     *   | * 5c5a667 (tag: tag2) Commit yyo
     *   | * 1560350 (tag: tag1) Commit ehw
     *   |/  
     *   | * 5abba0a (someotherbranch) Untagged commit #3 in branch someotherbranch
     *   | * a04254a Untagged commit #2 in branch someotherbranch
     *   | * 0a10479 Untagged commit #1 in branch someotherbranch
     *   | | * 5245a52 (feature/IN-12345) Untagged commit #3 in branch feature/IN-12345
     *   | | * 4d5a3c2 Untagged commit #2 in branch feature/IN-12345
     *   | | * c0ec7d2 Untagged commit #1 in branch feature/IN-12345
     *   | |/  
     *   |/|   
     *   | | * f14c4c5 (feature/SSO) Untagged commit in branch feature/SSO
     *   | | * fd313d1 (tag: 0.0.6-featuresso.2) Commit swa
     *   | | * 0c93cdc (tag: 0.0.6-featuresso.1) Commit nxl
     *   | |/  
     *   |/|   
     *   * | c0cface (integration) Untagged commit in branch integration
     *   * | db26bd2 (tag: 0.0.6-integration.2) Commit xkk
     *   |/  
     *   * 8673f95 (tag: 0.0.6-integration.1, master) Commit jad
     *   | * fe6db90 (fix-98765) Untagged commit in branch fix-98765
     *   | * f352f79 (tag: 0.0.8-fix98765.2) Commit fee
     *   | * 4b5687a (tag: 0.0.8-fix98765.1) Commit ogx
     *   | * 890e188 (v0.x) Untagged commit in branch v0.x
     *   | * d69e85b (tag: 0.0.7-v0x.1) Commit vvv
     *   | * 47aa910 (tag: 0.0.7) Commit uie
     *   | * 5b54a35 (tag: 0.0.6-v0x.3) Commit azr
     *   | * 39e3879 (tag: 0.0.6-v0x.2) Commit hdl
     *   | * 40c12cf (tag: 0.0.6-v0x.1) Commit aqi
     *   |/  
     *   | * 7505531 (rel/1.x) Untagged commit #3 in branch rel/1.x
     *   | * 82eab49 Untagged commit #2 in branch rel/1.x
     *   | * fb6fa06 Untagged commit #1 in branch rel/1.x
     *   |/  
     *   | * c55f82d (development) Untagged commit #3 in branch development
     *   | * dd47ac7 Untagged commit #2 in branch development
     *   | * 913e54e Untagged commit #1 in branch development
     *   | | * bc81e17 (rel/0.x) Untagged commit in branch rel/0.x
     *   | | * 636dff5 (tag: 0.0.6-rel.2) Commit kos
     *   | | * 99ae8fd (tag: 0.0.6-rel.1) Commit ecs
     *   | |/  
     *   |/|   
     *   | | * 290ef69 (v1.x) Untagged commit #3 in branch v1.x
     *   | | * b0e518b Untagged commit #2 in branch v1.x
     *   | | * b558e3d Untagged commit #1 in branch v1.x
     *   | |/  
     *   |/|   
     *   * | b282b60 Untagged commit in branch master
     *   * | 1a775e1 (tag: 0.0.5) Commit uap
     *   * | a7e5922 (tag: 0.0.4) Commit iqy
     *   * | 0861112 (tag: 0.0.3) Commit els
     *   * | 4d725fd (tag: 0.0.2) Commit wrj
     *   * | 330768c (tag: 0.0.1) Commit zwj
     *   |/  
     *   | * 341ce36 (main) Untagged commit #3 in branch main
     *   | * 51fac9e Untagged commit #2 in branch main
     *   | * a985699 Untagged commit #1 in branch main
     *   |/  
     *   * eaaa818 Initial commit
     * </pre>
     */
    EXTENDED_PRESET_BRANCHES_SHORT_UNMERGED( f -> Script.fromScratch(f).andAddFiles()
        .andCommit("Initial commit")
        .inBranch("master")
        // feed the MAINLINE branch: main
        .inBranch("main")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch main")
        .andAddFiles().andStage().andCommit("Untagged commit #2 in branch main")
        .andAddFiles().andStage().andCommit("Untagged commit #3 in branch main")
        // feed the INTEGRATION branch: development (forking from master)
        .inBranch("master")
        .inBranch("development")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch development")
        .andAddFiles().andStage().andCommit("Untagged commit #2 in branch development")
        .andAddFiles().andStage().andCommit("Untagged commit #3 in branch development")
        // feed the MAINLINE branch: master
        .inBranch("master")
        .andCommitWithTagInBranch("master", "0.0.1")
        .andCommitWithTagInBranch("master", "0.0.2", "Annotated tag to commit 0.0.2")
        .andCommitWithTagInBranch("master", "0.0.3")
        .andCommitWithTagInBranch("master", "0.0.4", "Annotated tag to commit 0.0.4")
        .andCommitWithTagInBranch("master", "0.0.5")
        .andAddFiles().andStage().andCommit("Untagged commit in branch master")
        // feed the MAINTENANCE branch: v0.x (forking from master)
        .inBranch("master")
        .andCommitWithTagInBranch("v0.x", "0.0.6-v0x.1", "Annotated tag to commit 0.0.6-v0x.1")
        .andCommitWithTagInBranch("v0.x", "0.0.6-v0x.2", "Annotated tag to commit 0.0.6-v0x.2")
        .andCommitWithTagInBranch("v0.x", "0.0.6-v0x.3")
        .andCommitWithTagInBranch("v0.x", "0.0.7", "Annotated tag to commit 0.0.7")
        .andCommitWithTagInBranch("v0.x", "0.0.7-v0x.1", "Annotated tag to commit 0.0.7-v0x.1")
        .andAddFiles().andStage().andCommit("Untagged commit in branch v0.x")
        // feed the MAINTENANCE branch: v1.x (forking from master)
        .inBranch("master")
        .inBranch("v1.x")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch v1.x")
        .andAddFiles().andStage().andCommit("Untagged commit #2 in branch v1.x")
        .andAddFiles().andStage().andCommit("Untagged commit #3 in branch v1.x")
        // feed the RELEASE branch: rel/0.x (forking from master)
        .inBranch("master")
        .andCommitWithTagInBranch("rel/0.x", "0.0.6-rel.1", "Annotated tag to commit 0.0.6-rel.1")
        .andCommitWithTagInBranch("rel/0.x", "0.0.6-rel.2")
        .andAddFiles().andStage().andCommit("Untagged commit in branch rel/0.x")
        // feed the RELEASE branch: rel/1.x (forking from master)
        .inBranch("master")
        .inBranch("rel/1.x")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch rel/1.x")
        .andAddFiles().andStage().andCommit("Untagged commit #2 in branch rel/1.x")
        .andAddFiles().andStage().andCommit("Untagged commit #3 in branch rel/1.x")
        // feed the INTEGRATION branch: integration (forking from master)
        .inBranch("master")
        .andCommitWithTagInBranch("integration", "0.0.6-integration.1", "Annotated tag to commit 0.0.6-integration.1")
        .andMergeInto("master")
        .inBranch("integration")
        .andCommitWithTagInBranch("integration", "0.0.6-integration.2")
        .andAddFiles().andStage().andCommit("Untagged commit in branch integration")
        // feed the FEATURE branch: feature/SSO (forking from integration)
        .inBranch("integration")
        .andCommitWithTagInBranch("feature/SSO", "0.0.6-featuresso.1", "Annotated tag to commit 0.0.6-featuresso.1")
        .andCommitWithTagInBranch("feature/SSO", "0.0.6-featuresso.2")
        .andAddFiles().andStage().andCommit("Untagged commit in branch feature/SSO")
        // feed the FEATURE branch: feature/IN-12345 (forking from integration)
        .inBranch("integration")
        .inBranch("feature/IN-12345")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch feature/IN-12345")
        .andAddFiles().andStage().andCommit("Untagged commit #2 in branch feature/IN-12345")
        .andAddFiles().andStage().andCommit("Untagged commit #3 in branch feature/IN-12345")
        // feed the HOTFIX branch: fix-98765 (forking from v0.x)
        .inBranch("v0.x")
        .andCommitWithTagInBranch("fix-98765", "0.0.8-fix98765.1", "Annotated tag to commit 0.0.8-fix98765.1")
        .andCommitWithTagInBranch("fix-98765", "0.0.8-fix98765.2")
        .andAddFiles().andStage().andCommit("Untagged commit in branch fix-98765")
        // feed the MATURITY branch: alpha (forking from integration)
        .inBranch("integration")
        .andCommitWithTagInBranch("alpha", "0.0.6-alpha.1", "Annotated tag to commit 0.0.6-alpha.1")
        .andCommitWithTagInBranch("alpha", "0.0.6-alpha.2")
        .andAddFiles().andStage().andCommit("Untagged commit in branch alpha")
        // feed the MATURITY branch: beta (forking from alpha)
        .inBranch("alpha")
        .andCommitWithTagInBranch("beta", "0.0.6-beta.1", "Annotated tag to commit 0.0.6-beta.1")
        .andCommitWithTagInBranch("beta", "0.0.6-beta.2")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch beta")
        // feed the MATURITY branch: gamma (forking from integration)
        .inBranch("integration")
        .inBranch("gamma")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch gamma")
        .andAddFiles().andStage().andCommit("Untagged commit #2 in branch gamma")
        .andAddFiles().andStage().andCommit("Untagged commit #3 in branch gamma")
        // feed the INTERNAL branch: internal
        .inBranch("integration")
        .inBranch("internal")
        .andCommitWithTagInBranch("internal", "0.0.6-internal.1+timestamp.001")
        .andCommitWithTagInBranch("internal", "0.0.6-internal.1+timestamp.002")
        .andCommitWithTagInBranch("internal", "0.0.6-internal.1+timestamp.003")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch internal")
        // feed the INTERNAL branch: somebranch
        .inBranch("integration")
        .inBranch("somebranch")
        .andCommitWithTagInBranch("somebranch", "tag1")
        .andCommitWithTagInBranch("somebranch", "tag2")
        .andCommitWithTagInBranch("somebranch", "tag3")
        // feed the INTERNAL branch: someotherbranch
        .inBranch("master")
        .inBranch("someotherbranch")
        .andAddFiles().andStage().andCommit("Untagged commit #1 in branch someotherbranch")
        .andAddFiles().andStage().andCommit("Untagged commit #2 in branch someotherbranch")
        .andAddFiles().andStage().andCommit("Untagged commit #3 in branch someotherbranch")
        .inBranch("master")
    );

    /**
     * The function to run in order to apply the scenario to a given directory. The function
     * takes the Git repository directory as input and returns the script used to realize
     * the scenario.
     */
    private final ScenarioFunction function;

    /**
     * Constructor.
     * 
     * @param function the function to run in order to apply the scenario to a given directory.
     */
    private Scenario(ScenarioFunction function) {
        Objects.requireNonNull(function, "Scenario can't be built with a null function");
        this.function = function;
    }

    /**
     * Applies the scenario in the given directory and returns the script that was used.
     * The returned script can be used to inspect the repository or perform further actions.
     * 
     * @param the directory to apply the scenario in. It must be not {@code null}, it must exist
     * and contain a valid Git repository. Previous history of the repository is left unchanged.
     * The script is being applied starting from the current branch in the given repository.
     * 
     * @return the script used to realize the scenario. It can be used for further operations.
     * 
     * @throws Exception in case of any issue.
     */
    public Script apply(File directory)
        throws Exception {
        return function.apply(directory);
    }

    /**
     * Realizes the scenario in a new temporary directory and returns the script that was used.
     * The returned script can be used to inspect the repository or perform further actions.
     * 
     * @return the script used to realize the scenario. It can be used for further operations.
     * 
     * @throws Exception in case of any issue.
     */
    public Script realize()
        throws Exception {
        return function.apply(FileSystemUtil.newTempDirectory(null, "scenario"));
    }

    /**
     * The function that realizes a scenario in a given directory and returns the script
     * used to realize the scenario.
     * 
     * This functional interface looks pretty much like {@link java.util.function.Function} but
     * allows exceptions to be thrown.
     */
    @FunctionalInterface
    private interface ScenarioFunction {
        /**
         * Applies the function to the given directory and returns the script used to
         * realize the scenario.
         * 
         * @param directory the directory to apply the scenario in
         * 
         * @return the script used to apply the scenario
         * 
         * @throws Exception in case of any issue
         */
        Script apply(File directory)
            throws Exception;
    }
}
