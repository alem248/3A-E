from django import forms
from django.contrib.auth.forms import AuthenticationForm
from django.core.exceptions import ValidationError


class PanelAuthenticationForm(AuthenticationForm):
    """Authenticate a Django account and verify the selected panel role."""

    account_type = forms.ChoiceField(
        label="Tipo de acceso",
        choices=(("usuario", "Usuario"), ("admin", "Administrador")),
        initial="usuario",
        widget=forms.RadioSelect,
    )

    def clean(self):
        cleaned_data = super().clean()
        user = self.get_user()
        account_type = cleaned_data.get("account_type")

        if user is None or account_type is None:
            return cleaned_data

        if account_type == "admin" and not user.is_staff:
            self.add_error("account_type", "Esta cuenta no tiene permisos de administrador.")
        elif account_type == "usuario" and user.is_staff:
            self.add_error("account_type", "Selecciona Administrador para esta cuenta.")

        return cleaned_data
