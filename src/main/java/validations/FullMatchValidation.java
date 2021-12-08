package validations;

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

import java.util.ArrayList;
import java.util.List;

public class FullMatchValidation {

    public static void main(String[] args) {
        JMorfSdk jMorfSdk = JMorfSdkFactory.loadFullLibrary();
        SyntaxParser sp = new SyntaxParser();
        sp.init();

/**
 * собирать в список не совпавшие - кандидаты на ошибки
 *  не по главным словам о/о
 *  hashCode
 */

//      найдено совпадение зависимых слов и их главные слова соответстсвуют
        String term = "системы";
        String text = "Предложение без термина. Открытые системы характеризуются обменом вещества. " +
                "Еще одно предложение без термина. Открытые атомы обмениваются веществом. Открытая система характеризуется" +
                " другими свойствами.";
        findMatch(term, text, jMorfSdk, sp);
        System.out.println("\n");

//      не найдено совпадений зависимых слов
        String term2 = "боль";
        String text2 = "Головные боли входят в число самых распространенных расстройств нервной системы." +
                "Головная боль испытывается половиной взрослых людей.";
//        findMatch(term2, text2, jMorfSdk, sp);
        System.out.println("\n");

//      найдено совпадение зависимых слов и их главные слова соответстсвуют
        String term3 = "пандемия";
        String text3 = "Пандемия коронавируса оказала серьезное негативное воздействие на системы здравоохранения многих стран. " +
                "Пандемия нового коронавируса способна подорвать глобальные усилия стран по борьбе с многими инфекционными заболеваниями.";
//        findMatch(term3, text3, jMorfSdk, sp);
        System.out.println("\n");

//      совпадение с термином найдено во втором предложении
        String term4 = "заседание";
        String text4 = "Сегодня состоялось очередное заседание Антинаркотической комиссии в Домодедово. " +
                "Заседание было посвящено подведению итогов межведомственной работы по профилактике наркомании.";
//        findMatch(term4, text4, jMorfSdk, sp);

    }

    public static void findMatch(String term, String text, JMorfSdk jMorfSdk, SyntaxParser sp) {
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
        List<String> errorList = new ArrayList<>();

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
                errorList.addAll(loop(dependentsOfOmoForm, dependentsOfTerm, errorList));
//              Для каждого зависимого слова из списка
//                for (OmoFormExt dependentOfOmoForm : dependentsOfOmoForm) {
//                    System.out.println(recursiveComparison(dependentOfOmoForm, dependentsOfTerm, errorList));
//                    System.out.println("cur: " + current);

//                  Пербираем стартовые зависимые слова из о/о для термина (из 2 пункта)
//                    for (OmoFormExt first : firstDependents) {
////                        System.out.println("fir: " + first);
//
////                      Сравниваем слова из стартовой и текущей омоформ (в начальной форме)
////                      Если зависимые равны
//                        if (first.getCurrencyOmoForm().getInitialForm().hashCode() ==
//                                current.getCurrencyOmoForm().getInitialForm().hashCode()) {
////                          Проверяем главные слова зависимых (термин и текущей зависимости)
////                          Взять у главного его зависимые
//                            if (first.getMainWord().getCurrencyOmoForm().getInitialForm().hashCode()
//                                    == current.getMainWord().getCurrencyOmoForm().getInitialForm().hashCode()) {
////                              При совпадении - вывод о том, что термин употреблен правильно
//                                System.out.println("Главное: " + current.getMainWord().getCurrencyOmoForm().getInitialForm().getInitialFormString());
//                                System.out.println("Зависимое: " + current.getCurrencyOmoForm().getInitialForm().getInitialFormString());
//                                System.out.println("Хеши главных слов совпадают.");
//                            } else {
////                              Либо не правильно, тогда записываем о/о в список
//                                errorList.add(String.valueOf(i));
//                                System.out.println("Главное: " + current.getMainWord().getCurrencyOmoForm().getInitialForm().getInitialFormString());
//                                System.out.println("Зависимое: " + current.getCurrencyOmoForm().getInitialForm().getInitialFormString());
//                                System.out.println("Хеши главных слов не совпадают.");
//                            }
//                        }
//                    }
//                }
            }
        }
        System.out.println("\nError list: " + errorList);
    }

    public static List<String> loop(List<OmoFormExt> dependentsOfOmoForm, List<OmoFormExt> dependentsOfTerm,
                                    List<String> errorList) {
        for (OmoFormExt dependentOfOmoForm : dependentsOfOmoForm) { // по каждому зависимому о/о
            GetCharacteristics current = dependentOfOmoForm.getCurrencyOmoForm().getInitialForm();
            for (OmoFormExt dependentOfTerm : dependentsOfTerm) { // по каждому зависимому термина
                GetCharacteristics term = dependentOfTerm.getCurrencyOmoForm().getInitialForm();
                if (current.getInitialFormKey() == term.getInitialFormKey()) { // сравниваем зависимые о/о и термина
                    GetCharacteristics mwOfDependentOfTerm = dependentOfTerm.getMainWord().getCurrencyOmoForm().getInitialForm(); // берем гс зависимого термина
                    GetCharacteristics mwOfDependentOmoForm = dependentOfOmoForm.getMainWord().getCurrencyOmoForm().getInitialForm(); // берем гс зависимого о/о
                    System.out.printf("Зависимое о/о: %s, зависимое термина: %s.\n", current.getInitialFormString(), term.getInitialFormString());
                    if (mwOfDependentOmoForm.getInitialFormKey() != mwOfDependentOfTerm.getInitialFormKey()) { // сравниваем гс
                        System.out.printf("ГС о/о: %s, ГС термина: %s.\n", mwOfDependentOmoForm.getInitialFormString(), mwOfDependentOfTerm.getInitialFormString());
                        errorList.add(String.format("Ожидание: %s, реальность: %s.", term.getInitialFormString(), current.getInitialFormString()));
                    }
                }

            }
            if (!dependentOfOmoForm.getDependentWords().isEmpty()) { // если у текущего зависимого есть еще зависимые
                errorList.addAll(loop(dependentOfOmoForm.getDependentWords(), dependentsOfTerm, errorList));
            }
        }

        return errorList;
    }

}
