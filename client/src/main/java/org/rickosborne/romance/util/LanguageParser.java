package org.rickosborne.romance.util;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Dictionaries;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.rickosborne.romance.db.model.BookModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.rickosborne.romance.util.StringStuff.firstWordOf;
import static org.rickosborne.romance.util.StringStuff.joinSorted;
import static org.rickosborne.romance.util.StringStuff.pairMap;

@Slf4j
public class LanguageParser {
    private static final Set<String> IGNORED_PRONOUNS = Set.of("i", "me", "my", "we", "you", "y'all");
    private static final Set<String> OBJECTS = Set.of("aer", "em", "faer", "her", "him", "hir", "per", "them", "ver", "xem");
    private static final Map<String, String> OBJECT_FROM_SUBJECT = pairMap(
        "ae", "aer", "e", "em", "ey", "em", "fae", "faer",
        "he", "him", "it", "it", "per", "per", "she", "her",
        "they", "them", "ve", "ver", "xe", "xem", "ze", "hir", "zie", "hir"
    );
    private static final Set<String> POSSESSIVES = Set.of("aer", "eir", "faer", "her", "hir", "his", "pers", "their", "vis", "xyr");
    private static final Set<String> POSSESSIVE_PRONOUNS = Set.of("aers", "eirs", "faers", "hers", "hirs", "his", "pers", "theirs", "vis", "xyrs");
    private static final Set<String> REFLEXIVE = Set.of("aerself", "eirs", "faerself", "herself", "hirself", "himself", "perself", "themself", "verself", "xemself");
    private static final Set<String> SUBJECTS = Set.of("ae", "e", "ey", "fae", "he", "it", "per", "she", "they", "ve", "xe", "ze", "zie");
    private static final Map<String, String> SUBJECT_FROM_OBJECT = pairMap(
        "aer", "ae", "em", "ey", "faer", "fae", "her", "she",
        "him", "he", "hir", "ze", "per", "per", "them", "they",
        "ver", "ve", "xem", "xe"
    );
    private static final Map<String, String> SUBJECT_FROM_POSSESSIVE = pairMap(
        "aer", "ae", "eir", "ey", "faer", "fae", "her", "she", "hir", "xe",
        "his", "he", "pers", "per", "their", "they", "vis", "ve", "xyr", "xe"
    );
    private static final Map<String, String> SUBJECT_FROM_POSSESSIVE_PRONOUN = pairMap(
        "aers", "ae", "eirs", "ey", "faers", "fae", "hers", "she", "hirs", "xe",
        "his", "he", "pers", "per", "theirs", "they", "vis", "ve", "xyrs", "xe"
    );
    private static final Map<String, String> SUBJECT_FROM_REFLEXIVE = pairMap(
        "aerself", "ae", "eirs", "ey", "faerself", "fae", "herself", "she",
        "hirself", "ze", "himself", "he", "perself", "per", "themself", "they",
        "verself", "ve", "xemself", "xe"
    );
    private static final Set<String> ignoredRelations = Set.of(
        "per:other_family", "per:siblings", "per:origin",
        "per:children", "per:parents", "per:spouse",
        "per:employee_or_member_of", "org:shareholders", "org:website"
    );
    private static final Properties nlpProps = PropertiesUtils.asProperties(
        "annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref,kbp,natlog,openie,sentiment",
        "ner.combinationMode", "HIGH_RECALL",
        "coref.algorithm", "neural"
    );

    private static void mergeCounts(@NonNull final Map<String, Integer> into, @NonNull final Map<String, Integer> source) {
        source.forEach((key, count) -> into.compute(key, (k, p) -> (p == null ? 0 : p) + count));
    }

    private static String most(@NonNull final Map<String, Integer> counted) {
        if (counted.isEmpty()) {
            return null;
        }
        return counted.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    @Getter(lazy = true)
    private final StanfordCoreNLP nlp = new StanfordCoreNLP(nlpProps);

    private void eachObject(
        @NonNull final RelationTriple relation,
        @NonNull final List<Ent> entities,
        @NonNull final BiConsumer<String, Ent> block
    ) {
        for (final CoreLabel objectLabel : relation.canonicalObject) {
            final String name = objectLabel.originalText().toLowerCase();
            final Ent person = entityNamed(name, entities);
            if (person != null) {
                final String subject = relation.subjectLemmaGloss().toLowerCase();
                block.accept(subject, person);
            }
        }
    }

    private void eachSubject(
        @NonNull final RelationTriple relation,
        @NonNull final List<Ent> entities,
        @NonNull final TriConsumer<String, String, Ent> block
    ) {
        for (final CoreLabel subjectLabel : relation.canonicalSubject) {
            final String name = subjectLabel.originalText().toLowerCase();
            final Ent person = entityNamed(name, entities);
            if (person != null) {
                final String object = relation.objectLemmaGloss().toLowerCase();
                block.accept(object, relation.objectLemmaGloss(), person);
            }
        }
    }

    private Ent entityNamed(@NonNull final String name, @NonNull final List<Ent> entities) {
        final String lc = name.toLowerCase();
        return entities.stream().filter(e -> e.aliases.contains(lc)).findAny().orElse(null);
    }

    private <K> Integer incrementCount(final K key, final Integer previous) {
        return (previous == null ? 0 : previous) + 1;
    }

    public Summary summarize(@NonNull final String text) {
        final CoreDocument document = new CoreDocument(text);
        getNlp().annotate(document);
        final List<Ent> entities = new LinkedList<>();
        for (final CorefChain chain : document.corefChains().values()) {
            final CorefChain.CorefMention rep = chain.getRepresentativeMention();
            if ((rep.animacy == Dictionaries.Animacy.INANIMATE && rep.mentionType != Dictionaries.MentionType.PROPER) || rep.number == Dictionaries.Number.PLURAL) {
                continue;
            }
            final Ent person = new Ent();
            for (final CorefChain.CorefMention mention : chain.getMentionsInTextualOrder()) {
                if (mention.animacy != Dictionaries.Animacy.ANIMATE || mention.number != Dictionaries.Number.SINGULAR) {
                    continue;
                }
                if (mention.mentionType == Dictionaries.MentionType.PROPER) {
                    final String name = mention.mentionSpan;
                    if (!name.contains("'s") && !name.contains("’s") && !name.contains(",")) {
                        final String firstName = firstWordOf(name);
                        person.nameCounts.compute(firstName, this::incrementCount);
                        person.aliases.add(firstName.toLowerCase());
                    }
                } else if (mention.mentionType == Dictionaries.MentionType.PRONOMINAL) {
                    final String pronoun = mention.mentionSpan.toLowerCase();
                    if (OBJECTS.contains(pronoun)) {
                        person.objectPronounCounts.compute(pronoun, this::incrementCount);
                    } else if (SUBJECTS.contains(pronoun)) {
                        person.subjectPronounCounts.compute(pronoun, this::incrementCount);
                    } else if (REFLEXIVE.contains(pronoun)) {
                        person.subjectPronounCounts.compute(SUBJECT_FROM_REFLEXIVE.get(pronoun), this::incrementCount);
                    } else if (POSSESSIVE_PRONOUNS.contains(pronoun)) {
                        person.subjectPronounCounts.compute(SUBJECT_FROM_POSSESSIVE_PRONOUN.get(pronoun), this::incrementCount);
                    } else if (POSSESSIVES.contains(pronoun)) {
                        person.subjectPronounCounts.compute(SUBJECT_FROM_POSSESSIVE.get(pronoun), this::incrementCount);
                    } else if (!IGNORED_PRONOUNS.contains(pronoun)) {
                        log.warn("Unknown pronoun: " + pronoun);
                    }
                }
            }
            final String name = person.getName();
            if (name == null) {
                continue;
            }
            final Ent existingEnt = entityNamed(name, entities);
            if (existingEnt != null) {
                existingEnt.mergeFrom(person);
                continue;
            }
            log.info("Entity: " + person);
            entities.add(person);
        }
        for (final CoreSentence sentence : document.sentences()) {
            for (final RelationTriple relation : sentence.relations()) {
                final String rel = relation.relationGloss();
                if (ignoredRelations.contains(rel)) {
                    continue;
                }
                if ("per:title".equals(rel)) {
                    eachSubject(relation, entities, (title, titleTitle, ent) -> ent.professions.add(titleTitle));
                } else if ("org:top_members_employees".equals(rel)) {
                    eachObject(relation, entities, (org, ent) -> ent.professions.add("boss/CEO/founder"));
                } else if ("per:age".equals(rel)) {
                    eachSubject(relation, entities, (age, ageTitle, ent) -> ent.ages.add(age));
                } else if (rel.contains("of_residence") || rel.contains("of_headquarters")) {
                    eachSubject(relation, entities, (location, locationTitle, ent) -> ent.locations.add(locationTitle));
                } else {
                    log.info("RELATION: " + rel);
                }
            }
        }
        final List<BookModel.MainChar> mcs = entities.stream()
            .map(e -> new BookModel.MainChar(joinSorted(", ", e.getAges()), null, e.getGender(), e.getName(), joinSorted(" ", e.getProfessions()), e.getPronouns()))
            .sorted(Comparator.comparing(BookModel.MainChar::getName))
            .collect(Collectors.toList());
        log.info("MCs: " + mcs);
        final String location = joinSorted(", ", entities.stream().flatMap(e -> e.locations.stream()).collect(Collectors.toSet()));
        if (location != null) {
            log.info("Location: " + location);
        }
        return new Summary(document, mcs, location);
    }

    @Value
    @Builder(toBuilder = true)
    private static class Ent {
        Set<String> ages = new HashSet<>();
        Set<String> aliases = new HashSet<>();
        Set<String> locations = new HashSet<>();
        Map<String, Integer> nameCounts = new HashMap<>();
        Map<String, Integer> objectPronounCounts = new HashMap<>();
        Set<String> professions = new HashSet<>();
        Map<String, Integer> subjectPronounCounts = new HashMap<>();

        public String getGender() {
            final String objectPronoun = getObjectPronoun();
            final String subjectPronoun = getSubjectPronoun();
            return (objectPronoun == null || subjectPronoun == null) ? null
                : "she".equals(subjectPronoun) ? ("her".equals(objectPronoun) ? "F" : "F|NB")
                : "he".equals(subjectPronoun) ? ("him".equals(objectPronoun) ? "M" : "M|NB")
                : "NB";
        }

        public String getName() {
            return most(nameCounts);
        }

        public String getObjectPronoun() {
            final String objectPronoun = most(objectPronounCounts);
            if (objectPronoun == null && !subjectPronounCounts.isEmpty()) {
                return OBJECT_FROM_SUBJECT.get(getSubjectPronoun());
            }
            return objectPronoun;
        }

        public String getPronouns() {
            final String objectPronoun = getObjectPronoun();
            final String subjectPronoun = getSubjectPronoun();
            return objectPronoun == null && subjectPronoun == null ? null
                : objectPronoun == null ? subjectPronoun
                : subjectPronoun == null ? objectPronoun
                : (subjectPronoun + "/" + objectPronoun);
        }

        public String getSubjectPronoun() {
            final String subjectPronoun = most(subjectPronounCounts);
            if (subjectPronoun == null && !objectPronounCounts.isEmpty()) {
                return SUBJECT_FROM_OBJECT.get(getObjectPronoun());
            }
            return subjectPronoun;
        }

        public void mergeFrom(@NonNull final Ent other) {
            ages.addAll(other.ages);
            aliases.addAll(other.aliases);
            professions.addAll(other.professions);
            mergeCounts(nameCounts, other.nameCounts);
            mergeCounts(objectPronounCounts, other.objectPronounCounts);
            mergeCounts(subjectPronounCounts, other.subjectPronounCounts);
        }

        @Override
        public String toString() {
            return getName()
                + Optional.ofNullable(getPronouns()).map(p -> " (" + p + ")").orElse("")
                + Optional.ofNullable(getGender()).map(g -> " " + g).orElse("");
        }
    }

    @Value
    public static class Summary {
        CoreDocument document;
        List<BookModel.MainChar> mainChars;
        String location;
    }
}
