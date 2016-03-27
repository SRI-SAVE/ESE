/*
 * Copyright 2016 SRI International
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sri.pal;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Set;

import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.pal.util.PALTestCase;
import com.sri.tasklearning.spine.impl.jms.util.MockSpine;
import com.sri.tasklearning.spine.messages.SerialNumberResponse;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.util.ReplyWatcher;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FileTypeStorage_Test
        extends PALTestCase {
    private static final String FMT_VERS = LumenProcedureDef.SERIALIZATION_FORMAT_VERSION;

    private static Bridge palBridge;
    private static ProcedureDef pd;
    private static MockSpine spine;
    private static File baseDir;
    private static SimpleTypeName name;
    private static String actionName;
    private static SimpleTypeName actionName4;
    private static String actionName2;
    private static ProcedureDef pd2;
    private static ProcedureDef pd4;
    private static FileTypeStorage fts;
    private static String actionName3;
    private static LumenProcedureDef pd3;

    @BeforeClass
    public static void setup()
            throws Exception {
        baseDir = new File("fileTypeStorageDir");
        baseDir.mkdir();
        spine = new MockSpine();
        ReplyWatcher<SerialNumberResponse> serialGetter = new ReplyWatcher<SerialNumberResponse>(
                SerialNumberResponse.class, spine);
        spine.subscribe(serialGetter, SystemMessageType.SERIAL_NUMBER_RESPONSE);
        Bridge.startPAL();
        palBridge = Bridge.newInstance("ftst");
        fts = new FileTypeStorage(baseDir, spine.getClientId());
        Assert.assertTrue(palBridge.setTypeStorage(fts));

        name = (SimpleTypeName) TypeNameFactory.makeName("proc1",
                LumenProcedureDef.SERIALIZATION_FORMAT_VERSION,
                LumenProcedureExecutor.NAMESPACE);
        actionName = name.getFullName();
        pd = LumenProcedureDef
                .newInstance(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                                + "<TaskModel version=\""
                                + FMT_VERS
                                + "\">\n"
                                + "    <cueSource></cueSource>\n"
                                + "    <bodySource>action '"
                                + actionName
                                + "'()\n"
                                + " execute:{\n"
                                + "}\n"
                                + " argtypes:[]\n"
                                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                                + "</TaskModel>", false, palBridge);

        actionName2 = "lumen^" + FMT_VERS + "^emptyProc2";
        pd2 = LumenProcedureDef
                .newInstance(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                                + "<TaskModel version=\""
                                + FMT_VERS
                                + "\">\n"
                                + "    <cueSource></cueSource>\n"
                                + "    <bodySource>action '"
                                + actionName2
                                + "'()\n"
                                + " execute:{\n"
                                + "}\n"
                                + " argtypes:[]\n"
                                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                                + "</TaskModel>", false, palBridge);

        actionName3 = actionName2;
        pd3 = LumenProcedureDef
                .newInstance(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                                + "<TaskModel version=\""
                                + FMT_VERS
                                + "\">\n"
                                + "    <cueSource></cueSource>\n"
                                + "    <bodySource>action '"
                                + actionName3
                                + "'()\n"
                                + " execute:{\n"
                                + "}\n"
                                + " argtypes:[]\n"
                                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                                + "</TaskModel>", false, palBridge);

        actionName4 = (SimpleTypeName) TypeNameFactory.makeName("proc.empty.2",
                FMT_VERS, "lumen");
        pd4 = LumenProcedureDef
                .newInstance(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                                + "<TaskModel version=\""
                                + FMT_VERS
                                + "\">\n"
                                + "    <cueSource></cueSource>\n"
                                + "    <bodySource>action '"
                                + actionName4.getFullName()
                                + "'()\n"
                                + " execute:{\n"
                                + "}\n"
                                + " argtypes:[]\n"
                                + " properties:{creator:\"user unknown\", demonstrated_variable_bindings:{}, learner:\"LAPDOG\", learner_version:\"Beta 5.0\", registration_date:\"20100115T003110Z\"};</bodySource>\n"
                                + "</TaskModel>", false, palBridge);

    }

    @AfterClass
    public static void cleanup()
            throws Exception {
        spine.shutdown(true);
        delete(baseDir);
    }

    @AfterMethod
    public void clearStorage()
            throws Exception {
        for (SimpleTypeName name : fts.listTypes()) {
            fts.putType(name, null);
        }
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                delete(subFile);
            }
        }
        assertTrue(file.delete());
    }

    private String mangle(String input) {
        return input.replaceAll("[^A-Za-z0-9]", "_");
    }

    @Test
    public void canConstruct()
            throws PALException {
        assertTrue(FileTypeStorage.class.isInstance(fts));
        assertNotNull(fts);
    }

    @Test
    public void savingFileAppendsExtension()
            throws PALException {
        fts.putType(name, pd.getXml());
        File nsDir = new File(baseDir, mangle(name.getNamespace()));
        File versDir = new File(nsDir, mangle(name.getVersion()));
        File fullFileName = new File(versDir, mangle(name.getSimpleName())
                + ".procedure");
        assertTrue(fullFileName.toString(), fullFileName.exists());
    }

    @Test
    public void loadingFileResolvesExtension()
            throws PALException {
        fts.putType(name, pd.getXml());

        Set<SimpleTypeName> typeSet = fts
                .listTypes(TypeStorage.Subset.PROCEDURE);

        assertTrue(typeSet.toString(),
                typeSet.contains(TypeNameFactory.makeName(actionName)));
    }

    @Test
    public void requestingFileWithPeriodInTypeNameWorks()
            throws PALException {
        fts.putType(actionName4, pd4.getXml());

        Set<SimpleTypeName> typeSet = fts
                .listTypes(TypeStorage.Subset.PROCEDURE);

        assertTrue(typeSet.toString(), typeSet.contains(actionName4));
    }

    /**
     * Requirements: TypeName typeName1 = "typename1" TypeName typeName2 =
     * "typename2"
     *
     * Filename filename1 = fileSystemSafe(typeName1) Filename filename2 =
     * fileSystemSafe(typeName1) Filename filename3 = fileSystemSafe(typeName2)
     *
     * filename1 == filename2 but filename3 != (filename1 or filename2)
     *
     * @throws PALException
     */
    @Test
    public void fileSystemSafeWorksAsExpected()
            throws PALException {
        // First make sure the file name generation works
        File file1 = fts.getNewFile(pd.getName(), ".procedure");
        File file2 = fts.getNewFile(pd2.getName(), ".procedure"); // same as
// actionName 3
        File file3 = fts.getNewFile(pd3.getName(), ".procedure"); // same as
// actionName 2

        assertEquals(file2, file3);
        assertTrue(!file1.equals(file2));
        assertTrue(!file1.equals(file3));

        // Now test the system in practice
        fts.putType(name, pd.getXml());
        fts.putType((SimpleTypeName) TypeNameFactory.makeName(actionName2),
                pd2.getXml());
        fts.putType((SimpleTypeName) TypeNameFactory.makeName(actionName3),
                pd3.getXml());

        Set<SimpleTypeName> typeSet = fts
                .listTypes(TypeStorage.Subset.PROCEDURE);

        // Even though we added 2 types, since the latter two had the same name
// they map to the same file
        assertEquals(2, typeSet.size());

        assertTrue(typeSet.contains(TypeNameFactory.makeName(actionName)));
        assertTrue(typeSet.contains(TypeNameFactory.makeName(actionName2)));
        assertTrue(typeSet.contains(TypeNameFactory.makeName(actionName3)));
    }
}
