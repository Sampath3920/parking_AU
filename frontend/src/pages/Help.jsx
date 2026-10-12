import { Link } from "react-router-dom";

function Help() {
    return (
        <div className="container py-4">
            <h2>Help</h2>
            <p className="text-muted">Short instructions for the parking system.</p>

            <div className="accordion mb-4" id="helpAccordion">
                {[
                    [
                        "How do I book a parking slot?",
                        <ol key="a">
                            <li>Open <strong>Vehicles</strong> and add your vehicle. Wait until the administrator approves it.</li>
                            <li>Open <strong>Book</strong>, choose the approved vehicle, the start and end time and a slot.</li>
                            <li>Open <strong>Payments</strong> and send your payment details for the booking.</li>
                            <li>The administrator checks the payment and approves the booking. You get a notification.</li>
                        </ol>
                    ],
                    [
                        "How do I use the QR code?",
                        <ol key="b">
                            <li>When your booking is approved, open <strong>QR</strong> and choose the booking. Your QR code is shown.</li>
                            <li>At the gate, show the QR code to the security guard. The guard scans it to check you in.</li>
                            <li>When you leave, show the same QR code again so the guard can check you out.</li>
                            <li>The QR code works only for your booking and only during its time window.</li>
                        </ol>
                    ],
                    [
                        "How does payment work?",
                        <p key="c">
                            This system uses a manual (demo) payment check. You pay outside the system, then send the
                            payment method and the reference number in <strong>Payments</strong>. Ticking the
                            confirmation box only says that your details are correct. The money counts only after the
                            administrator verifies it. If a payment is rejected you will see the reason in your
                            notifications.
                        </p>
                    ],
                    [
                        "How do I cancel a booking?",
                        <p key="d">
                            Open <strong>History</strong> and press Cancel next to the booking. A booking cannot be
                            cancelled after check-in.
                        </p>
                    ],
                    [
                        "Something went wrong or my vehicle was damaged",
                        <p key="e">
                            Open <Link to="/reports">Complaints and damage reports</Link>, choose the type and describe
                            what happened. The administrator will answer and you will get a notification.
                        </p>
                    ]
                ].map(([question, answer], index) => (
                    <div className="accordion-item" key={question}>
                        <h3 className="accordion-header">
                            <button
                                className={`accordion-button ${index === 0 ? "" : "collapsed"}`}
                                type="button"
                                data-bs-toggle="collapse"
                                data-bs-target={`#help-${index}`}
                                aria-expanded={index === 0}
                                aria-controls={`help-${index}`}
                            >
                                {question}
                            </button>
                        </h3>
                        <div
                            id={`help-${index}`}
                            className={`accordion-collapse collapse ${index === 0 ? "show" : ""}`}
                            data-bs-parent="#helpAccordion"
                        >
                            <div className="accordion-body">{answer}</div>
                        </div>
                    </div>
                ))}
            </div>

            <div className="card shadow-sm">
                <div className="card-body">
                    <h5>Need more help?</h5>
                    <p className="mb-0">
                        Ask the parking office or the administrator of your college, or send a message through{" "}
                        <Link to="/reports">Complaints and damage reports</Link>.
                    </p>
                </div>
            </div>
        </div>
    );
}

export default Help;
