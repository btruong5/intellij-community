/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.psi.impl.java.stubs.impl;

import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsJavaCodeReferenceElementImpl;
import com.intellij.psi.impl.java.stubs.JavaClassReferenceListElementType;
import com.intellij.psi.impl.java.stubs.PsiClassReferenceListStub;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class PsiClassReferenceListStubImpl extends StubBase<PsiReferenceList> implements PsiClassReferenceListStub {
  private final String[] myNames;
  private volatile PsiClassType[] myTypes;

  public PsiClassReferenceListStubImpl(@NotNull JavaClassReferenceListElementType type, StubElement parent, @NotNull String[] names) {
    super(parent, type);
    ObjectUtils.assertAllElementsNotNull(names);
    myNames = names;
  }

  @NotNull
  @Override
  public PsiClassType[] getReferencedTypes() {
    PsiClassType[] types = myTypes;
    if (types == null) {
      myTypes = types = createTypes();
    }
    return types.clone();
  }

  @NotNull
  private PsiClassType[] createTypes() {
    PsiClassType[] types = myNames.length == 0 ? PsiClassType.EMPTY_ARRAY : new PsiClassType[myNames.length];

    final boolean compiled = ((JavaClassReferenceListElementType)getStubType()).isCompiled(this);
    if (compiled) {
      for (int i = 0; i < types.length; i++) {
        types[i] = new PsiClassReferenceType(new ClsJavaCodeReferenceElementImpl(getPsi(), myNames[i]), null);
      }
    }
    else {
      final PsiElementFactory factory = JavaPsiFacade.getElementFactory(getProject());

      int nullCount = 0;
      final PsiReferenceList psi = getPsi();
      for (int i = 0; i < types.length; i++) {
        try {
          final PsiJavaCodeReferenceElement ref = factory.createReferenceFromText(myNames[i], psi);
          ((PsiJavaCodeReferenceElementImpl)ref).setKindWhenDummy(PsiJavaCodeReferenceElementImpl.Kind.CLASS_NAME_KIND);
          types[i] = factory.createType(ref);
        }
        catch (IncorrectOperationException e) {
          types[i] = null;
          nullCount++;
        }
      }

      if (nullCount > 0) {
        PsiClassType[] newTypes = new PsiClassType[types.length - nullCount];
        int cnt = 0;
        for (PsiClassType type : types) {
          if (type != null) newTypes[cnt++] = type;
        }
        types = newTypes;
      }
    }
    return types;
  }

  @NotNull
  @Override
  public String[] getReferencedNames() {
    return myNames.clone();
  }

  @NotNull
  @Override
  public PsiReferenceList.Role getRole() {
    return JavaClassReferenceListElementType.elementTypeToRole(getStubType());
  }

  @Override
  public String toString() {
    return "PsiRefListStub[" + getRole() + ':' + String.join(", ", myNames) + ']';
  }
}