package validations;

import java.util.ArrayList;
import java.util.List;

import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.loader.JMorfSdkFactory;
import ru.textanalysis.tawt.ms.external.sp.BearingPhraseExt;
import ru.textanalysis.tawt.ms.external.sp.OmoFormExt;
import ru.textanalysis.tawt.ms.grammeme.MorfologyParameters;
import ru.textanalysis.tawt.ms.internal.IOmoForm;
import ru.textanalysis.tawt.ms.internal.form.GetCharacteristics;
import ru.textanalysis.tawt.ms.internal.ref.RefOmoForm;
import ru.textanalysis.tawt.ms.storage.OmoFormList;
import ru.textanalysis.tawt.sp.api.SyntaxParser;

public class RecursiveValidator {
    private final List<String> errorList = new ArrayList<>();
    private static final SyntaxParser sp = new SyntaxParser();
    private static final JMorfSdk jMorfSdk = JMorfSdkFactory.loadFullLibrary();

    public List<String> findMatch(String term, String text) {
        sp.init();
//      1. Термин к начальной форме
        OmoFormList list = jMorfSdk.getAllCharacteristicsOfForm(term);
        String initialTerm = "";
        for (IOmoForm item : list) {
            if (item.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN) {
                initialTerm = item.getInitialFormString();
            }
        }

        List<BearingPhraseExt> phraseExt = sp.getTreeSentenceWithoutAmbiguity(text);
        List<OmoFormExt> dependentsOfTerm = new ArrayList<>();
        int firstTermSentence = 0;
//      2. Поиск первого о/о с термином и списка его зависимых слов (назовем этот список стартовым)
        while (dependentsOfTerm.isEmpty()) {
            quit:
            for (int i = 0; i < phraseExt.size(); i++) {
                for (OmoFormExt omoForm : phraseExt.get(i).getMainOmoForms()) {
                    RefOmoForm refOmoForm = omoForm.getCurrencyOmoForm();
                    if (initialTerm.equals(refOmoForm.getInitialFormString())) { // гл. слово о/о совпадает с термином
                        dependentsOfTerm.addAll(omoForm.getDependentWords()); // записать его зависимые слова
                        firstTermSentence = i;
                        break quit;
                    }
                }
            }
        }

        System.out.println("Зависимости первого соответствующего предложения:");
        dependentsOfTerm.forEach(first -> System.out.println(first.getCurrencyOmoForm()));

//      3. Поиск зависимых слов у о.оборотов в тексте и сравнение их с зависымыми термина firstDependents
//      Поиск начинается с о/о следующего после того, в котором найден термин
        for (int i = firstTermSentence + 1; i < phraseExt.size(); i++) {
//          "Омоформы" о/о
            List<OmoFormExt> omoForms = new ArrayList<>(phraseExt.get(i).getMainOmoForms());
            for (OmoFormExt omoForm : omoForms) {
//              Список зависимых слов в текущем о/о
                List<OmoFormExt> dependentsOfOmoForm = new ArrayList<>(omoForm.getDependentWords());
                loop(dependentsOfOmoForm, dependentsOfTerm);
            }
        }

        return errorList;
    }

    private void loop(List<OmoFormExt> dependentsOfOmoForm, List<OmoFormExt> dependentsOfTerm) {
        for (OmoFormExt dependentOfOmoForm : dependentsOfOmoForm) { // по каждому зависимому о/о
            GetCharacteristics current = dependentOfOmoForm.getCurrencyOmoForm().getInitialForm();
            for (OmoFormExt dependentOfTerm : dependentsOfTerm) { // по каждому зависимому термина
                GetCharacteristics term = dependentOfTerm.getCurrencyOmoForm().getInitialForm();
                if (current.getInitialFormKey() == term.getInitialFormKey()) { // сравниваем зависимые о/о и термина
                    GetCharacteristics mwOfDependentOfTerm = dependentOfTerm.getMainWord().getCurrencyOmoForm().getInitialForm(); // берем гс зависимого термина
                    GetCharacteristics mwOfDependentOmoForm = dependentOfOmoForm.getMainWord().getCurrencyOmoForm().getInitialForm(); // берем гс зависимого о/о
                    if (mwOfDependentOmoForm.getInitialFormKey() != mwOfDependentOfTerm.getInitialFormKey()) { // сравниваем гс
                        errorList.add(String.format("Ожидание: %s, реальность: %s.", mwOfDependentOfTerm.getInitialFormString(), mwOfDependentOmoForm.getInitialFormString()));
                    }
                }
            }
            if (!dependentOfOmoForm.getDependentWords().isEmpty()) { // если у текущего зависимого есть еще зависимые
                loop(dependentOfOmoForm.getDependentWords(), dependentsOfTerm);
            }
        }
    }

}