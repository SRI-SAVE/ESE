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

package com.sri.tasklearning.lumenpal.mock;

import com.sri.ai.lumen.atr.decl.ATRDecl;
import com.sri.ai.lumen.atr.decl.ATRTypeDeclaration;
import com.sri.pal.common.SimpleTypeName;
import com.sri.pal.common.TypeNameFactory;
import com.sri.tasklearning.mediators.LockingActionModel;
import com.sri.tasklearning.mediators.TypeFetcher;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.messages.TypeResult;
import com.sri.tasklearning.spine.util.ATRTestUtil;
import com.sri.tasklearning.spine.util.ReplyWatcher;

public class MockSimpleTypeFetcher extends TypeFetcher {

    public MockSimpleTypeFetcher(ReplyWatcher<TypeResult> typeReceiver,
                                 Spine spine,
                                 LockingActionModel actionModel)
            throws SpineException {
        super(spine, actionModel, typeReceiver);
    }

    /**
     * This mock type fetcher is used to simulate getting a type from the
     * spine
     * @param typeName the type to retrieve
     * @return the SpineType
     * @throws SpineException if something goes wrong.
     */
    @Override
    public ATRDecl getType(SimpleTypeName typeName) throws SpineException {
        if (typeName.equals(TypeNameFactory.makeName("List"))) {

            SimpleTypeName stringType = (SimpleTypeName) TypeNameFactory
                    .makeName("String");
            ATRTypeDeclaration stringSpineType = ATRTestUtil.makeCustomType(
                    stringType, String.class);

            return ATRTestUtil.makeList(
                    (SimpleTypeName) TypeNameFactory.makeName("List"),
                    stringSpineType);
        } else if (typeName.equals(TypeNameFactory.makeName("String"))) {
            SimpleTypeName stringType = (SimpleTypeName) TypeNameFactory
                    .makeName("String");
            return ATRTestUtil.makeCustomType(stringType, String.class);
        }
        return null;
    }
}
