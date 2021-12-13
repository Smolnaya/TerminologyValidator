package validations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.inject.internal.util.Lists;
import com.google.inject.internal.util.Sets;
import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.loader.JMorfSdkFactory;
import ru.textanalysis.tawt.ms.external.sp.BearingPhraseExt;
import ru.textanalysis.tawt.ms.external.sp.OmoFormExt;
import ru.textanalysis.tawt.ms.grammeme.MorfologyParameters;
import ru.textanalysis.tawt.ms.internal.IOmoForm;
import ru.textanalysis.tawt.ms.internal.form.GetCharacteristics;
import ru.textanalysis.tawt.ms.storage.OmoFormList;
import ru.textanalysis.tawt.sp.api.SyntaxParser;

public class TermValidator {
    private static final SyntaxParser sp = new SyntaxParser();
    private static final JMorfSdk jMorfSdk = JMorfSdkFactory.loadFullLibrary();

    public TermValidator() {
        sp.init();
    }

//    public void func() {
//        OmoFormList list = jMorfSdk.getAllCharacteristicsOfForm("включено");
//        OmoFormList list2 = jMorfSdk.getAllCharacteristicsOfForm("включено");
//        list.forEach(System.out::println);
//        list2.forEach(System.out::println);
//    }

    public List<String> findMatch(String term, String text) {
        List<String> errors = new ArrayList<>();
//      1. Термин к начальной форме
        OmoFormList list = jMorfSdk.getAllCharacteristicsOfForm(term);
        String initialTerm = "";
        for (IOmoForm item : list) {
            if (item.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN) {
                initialTerm = item.getInitialFormString();
            }
        }

//      2. Поиск первого о/о с термином и списка его зависимых слов (назовем этот список стартовым)
        List<BearingPhraseExt> phraseExts = sp.getTreeSentenceWithoutAmbiguity(text);
        List<OmoFormExt> dependentsOfTerm = getTermDependents(text, initialTerm);

//      3. Поиск зависимых слов у о.оборотов в тексте и сравнение их с зависымыми термина firstDependents
//      Поиск начинается с о/о следующего после того, в котором найден термин
        for (BearingPhraseExt bearingPhraseExt : phraseExts) {
//          "Омоформы" о/о
            List<OmoFormExt> omoForms = new ArrayList<>(bearingPhraseExt.getMainOmoForms());
            for (OmoFormExt omoForm : omoForms) {
//              Список зависимых слов в текущем о/о
                List<OmoFormExt> dependentsOfOmoForm = new ArrayList<>(omoForm.getDependentWords());
                errors.addAll(searchErrors(dependentsOfOmoForm, dependentsOfTerm, new ArrayList<>()));
            }
        }
        return errors;
    }

    private List<OmoFormExt> getTermDependents(String text, String term) {
        List<List<OmoFormExt>> termDependents = new ArrayList<>();
        List<BearingPhraseExt> phraseExts = sp.getTreeSentenceWithoutAmbiguity(text);
        exit:
        for (BearingPhraseExt phraseExt : phraseExts) {
            List<OmoFormExt> omoForms = phraseExt.getMainOmoForms();
            for (OmoFormExt ext : omoForms) {
                termDependents = searchTermDependents(ext.getDependentWords(), term, new ArrayList<>());
                break exit;
            }
        }
        return termDependents.get(0);
    }

    // рекусивный поиск зависимых оборотов для термина
    private List<List<OmoFormExt>> searchTermDependents(List<OmoFormExt> dependents, String term, List<List<OmoFormExt>> termDependents) {
        for (OmoFormExt dependent : dependents) {
            if (dependent.getCurrencyOmoForm().getInitialFormString().equals(term)) {
                termDependents.add(dependent.getDependentWords());
            }
            if (termDependents.isEmpty() && dependent.haveDep()) {
                searchTermDependents(dependent.getDependentWords(), term, termDependents);
            }
        }
        return termDependents;
    }

    // рекурсивный поиск ошибок применения термина
    private List<String> searchErrors(List<OmoFormExt> dependentsOfOmoForm, List<OmoFormExt> dependentsOfTerm, List<String> errors) {
        for (OmoFormExt dependentOfOmoForm : dependentsOfOmoForm) { // по каждому зависимому о/о
            GetCharacteristics current = dependentOfOmoForm.getCurrencyOmoForm().getInitialForm();
            for (OmoFormExt dependentOfTerm : dependentsOfTerm) { // по каждому зависимому термина
                GetCharacteristics term = dependentOfTerm.getCurrencyOmoForm().getInitialForm();
                if (current.getInitialFormKey() == term.getInitialFormKey()) { // сравниваем зависимые о/о и термина
                    GetCharacteristics mwOfDependentOfTerm = dependentOfTerm.getMainWord().getCurrencyOmoForm().getInitialForm(); // берем гс зависимого термина
                    GetCharacteristics mwOfDependentOmoForm = dependentOfOmoForm.getMainWord().getCurrencyOmoForm().getInitialForm(); // берем гс зависимого о/о
                    if (mwOfDependentOmoForm.getInitialFormKey() != mwOfDependentOfTerm.getInitialFormKey()) { // сравниваем гс
                        errors.add(String.format("Ожидание: %s, реальность: %s.", mwOfDependentOfTerm.getInitialFormString(), mwOfDependentOmoForm.getInitialFormString()));
                    }
                }
            }
            if (!dependentOfOmoForm.getDependentWords().isEmpty()) { // если у текущего зависимого есть еще зависимые
                searchErrors(dependentOfOmoForm.getDependentWords(), dependentsOfTerm, errors);
            }
        }
        return new ArrayList<>(new HashSet<>(errors));
    }

}
