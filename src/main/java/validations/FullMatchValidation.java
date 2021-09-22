package validations;

import ru.textanalysis.tawt.jmorfsdk.JMorfSdk;
import ru.textanalysis.tawt.jmorfsdk.loader.JMorfSdkFactory;
import ru.textanalysis.tawt.ms.external.sp.BearingPhraseExt;
import ru.textanalysis.tawt.ms.external.sp.OmoFormExt;
import ru.textanalysis.tawt.ms.grammeme.MorfologyParameters;
import ru.textanalysis.tawt.ms.internal.IOmoForm;
import ru.textanalysis.tawt.ms.internal.ref.RefOmoForm;
import ru.textanalysis.tawt.ms.storage.OmoFormList;
import ru.textanalysis.tawt.sp.api.SyntaxParser;

import java.util.ArrayList;
import java.util.List;

public class FullMatchValidation {

    public static void main(String[] args) {
        JMorfSdk jMorfSdk = JMorfSdkFactory.loadFullLibrary();
        SyntaxParser sp = new SyntaxParser();
        sp.init();

//      найдено совпадение зависимых слов и их главные слова соответстсвуют
        String term = "системы";
        String text = "Предложение без термина. Открытые системы характеризуются обменом вещества. " +
                "Еще одно предложение без термина. Открытые системы обмениваются веществом.";
        findMatch(term, text, jMorfSdk, sp);
        System.out.println("\n");

//      не найдено совпадений зависимых слов
        String term2 = "боль";
        String text2 = "Головные боли входят в число самых распространенных расстройств нервной системы." +
                "Головная боль испытывается половиной взрослых людей.";
        findMatch(term2, text2, jMorfSdk, sp);
        System.out.println("\n");

//      найдено совпадение зависимых слов и их главные слова соответстсвуют
        String term3 = "пандемия";
        String text3 = "Пандемия коронавируса оказала серьезное негативное воздействие на системы здравоохранения многих стран. " +
                "Пандемия нового коронавируса способна подорвать глобальные усилия стран по борьбе с многими инфекционными заболеваниями.";
        findMatch(term3, text3, jMorfSdk, sp);
        System.out.println("\n");

//      совпадение с термином найдено во втором предложении
        String term4 = "заседание";
        String text4 = "Сегодня состоялось очередное заседание Антинаркотической комиссии в Домодедово. " +
                "Заседание было посвящено подведению итогов межведомственной работы по профилактике наркомании.";
        findMatch(term4, text4, jMorfSdk, sp);

    }

    public static void findMatch(String term, String text, JMorfSdk jMorfSdk, SyntaxParser sp) {
//      Термин к начальной форме
        OmoFormList list = jMorfSdk.getAllCharacteristicsOfForm(term);
        String initialTerm = "";
        for (IOmoForm item : list) {
            if (item.getTypeOfSpeech() == MorfologyParameters.TypeOfSpeech.NOUN) {
                initialTerm = item.getInitialFormString();
            }
        }

//      Поиск первого совпадения главного слова опорного оборота с термином в тексте
        List<BearingPhraseExt> phraseExt = sp.getTreeSentenceWithoutAmbiguity(text);
        List<OmoFormExt> firstDependents = new ArrayList<>();
        int firstTermSentence = 0;

        while (firstDependents.isEmpty()) {
            List<OmoFormExt> omoForms = new ArrayList<>();
            quit:
            for (int i = 0; i < phraseExt.size(); i++) {
                omoForms.addAll(phraseExt.get(i).getMainOmoForms());
                for (OmoFormExt omoForm : omoForms) {
                    RefOmoForm refOmoForm = omoForm.getCurrencyOmoForm();
                    if (initialTerm.equals(refOmoForm.getInitialFormString())) { // гл. слово о.б. совпадает с термином
                        firstDependents.addAll(omoForm.getDependentWords()); // записать его зависимые слова
                        firstTermSentence = i;
                        System.out.println("Зависимости первого соответствующего предложения:");
                        firstDependents.forEach(first -> System.out.println(first.getCurrencyOmoForm()));
                        break quit;
                    }
                }
            }
        }

//      Поиск последующих совпадений зависимых слов
        for (int i = firstTermSentence + 1; i < phraseExt.size(); i++) {
            List<OmoFormExt> omoForms = new ArrayList<>(phraseExt.get(i).getMainOmoForms());
            for (OmoFormExt omoForm : omoForms) {
                List<OmoFormExt> currentDependents = new ArrayList<>(omoForm.getDependentWords());
                System.out.println("\tГлавное слово текущей зависимости:");
                currentDependents.forEach(current -> System.out.println("\t" + current.getMainWord().getCurrencyOmoForm()));
                System.out.println("\tЗависимость:");
                currentDependents.forEach(current -> System.out.println("\t" + current.getCurrencyOmoForm()));
                for (OmoFormExt current : currentDependents) {
                    for (OmoFormExt first : firstDependents) {
                        if (first.getCurrencyOmoForm().hashCode() == current.getCurrencyOmoForm().hashCode()) {
                            System.out.println("Найдено точное совпадение среди зависимых слов в предложении " + (i + 1));
                            System.out.println("\tзависимость: " + first.getCurrencyOmoForm());
                            if (first.getMainWord().getCurrencyOmoForm().hashCode()
                                    == current.getMainWord().getCurrencyOmoForm().hashCode()) {
                                System.out.println("\tХеши главных слов совпадают.");
                            } else {
                                System.out.println("\tХеши главных слов не совпадают.");
                            }
                        }
                    }
                }
            }
        }
    }

}
